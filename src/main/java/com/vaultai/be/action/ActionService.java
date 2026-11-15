package com.vaultai.be.action;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaultai.be.account.AccountDto;
import com.vaultai.be.account.AccountService;
import com.vaultai.be.account.TransactionDto;
import com.vaultai.be.account.TransferResult;
import com.vaultai.be.account.TransferService;
import com.vaultai.be.agent.ProposedAction;
import com.vaultai.be.audit.AuditEventType;
import com.vaultai.be.audit.AuditService;
import com.vaultai.be.common.BusinessRuleException;
import com.vaultai.be.common.NotFoundException;
import com.vaultai.be.config.AppProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * "The agent proposes, the backend decides":
 * read actions run right away, money actions wait for the user's explicit confirmation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionService {

    private final ActionValidator validator;
    private final AccountService accountService;
    private final TransferService transferService;
    private final AgentActionRepository actionRepository;
    private final AuditService auditService;
    private final AppProperties properties;

    @Transactional
    public ActionResultDto handleProposal(UUID ownerId, UUID conversationId, ProposedAction proposal) {
        ValidatedAction action;
        try {
            action = validator.validate(ownerId, proposal);
        } catch (ActionRejectedException ex) {
            ActionType type = proposal == null ? null : ActionType.parse(proposal.type()).orElse(null);
            auditService.record(ownerId, null, AuditEventType.ACTION_REJECTED,
                    proposal == null ? null : proposal.type(), ex.getMessage());
            return ActionResultDto.rejected(null, type, ex.getMessage());
        }

        return switch (action) {
            case ValidatedAction.GetBalance balance -> {
                AccountDto account = accountService.getAccount(ownerId, balance.accountId());
                auditService.record(ownerId, null, AuditEventType.ACTION_EXECUTED,
                        action.type().name(), "account=" + balance.accountId());
                yield ActionResultDto.completed(null, action.type(), "Balance retrieved", account);
            }
            case ValidatedAction.GetTransactions history -> {
                List<TransactionDto> transactions =
                        accountService.getTransactions(ownerId, history.accountId(), history.limit());
                auditService.record(ownerId, null, AuditEventType.ACTION_EXECUTED,
                        action.type().name(), "account=" + history.accountId());
                yield ActionResultDto.completed(null, action.type(), "Transactions retrieved", transactions);
            }
            case ValidatedAction.Transfer transfer -> createPendingTransfer(ownerId, conversationId, transfer);
        };
    }

    @Transactional
    public ActionResultDto confirm(UUID ownerId, UUID actionId, String idempotencyKey) {
        AgentAction action = actionRepository.findForUpdate(actionId, ownerId)
                .orElseThrow(() -> new NotFoundException("Action not found"));

        // the same request sent again (retry, double click): return the same result
        if (action.getStatus() == ActionStatus.EXECUTED && idempotencyKey.equals(action.getIdempotencyKey())) {
            return ActionResultDto.completed(action.getId(), action.getType(), action.getResultMessage(), null);
        }
        if (action.getStatus() != ActionStatus.PENDING) {
            throw new BusinessRuleException("This action is no longer pending (" + action.getStatus() + ")");
        }

        Instant now = Instant.now();
        if (action.isExpired(now)) {
            action.decide(ActionStatus.EXPIRED, "Confirmation window expired", now);
            auditService.record(ownerId, action.getId(), AuditEventType.ACTION_EXPIRED, action.getType().name(), null);
            return ActionResultDto.of(action.getId(), action.getType(), ActionOutcome.EXPIRED,
                    "The confirmation window has expired. Please ask the assistant again.");
        }

        ValidatedAction.Transfer transfer = action.toTransfer();
        try {
            validator.revalidateTransfer(ownerId, transfer);
        } catch (ActionRejectedException ex) {
            action.decide(ActionStatus.REJECTED, ex.getMessage(), now);
            auditService.record(ownerId, action.getId(), AuditEventType.ACTION_REJECTED,
                    action.getType().name(), ex.getMessage());
            return ActionResultDto.rejected(action.getId(), action.getType(), ex.getMessage());
        }

        TransferResult result = transferService.transfer(ownerId, transfer.fromAccountId(),
                transfer.toIban(), transfer.amount(), transfer.description());

        action.setIdempotencyKey(idempotencyKey);
        action.setResultTransactionId(result.transactionId());
        action.decide(ActionStatus.EXECUTED, "Transfer completed", now);
        auditService.record(ownerId, action.getId(), AuditEventType.ACTION_EXECUTED, action.getType().name(),
                "amount=%s %s, to=%s".formatted(transfer.amount(), transfer.currency(), transfer.toIban()));

        return ActionResultDto.completed(action.getId(), action.getType(), "Transfer completed", result);
    }

    @Transactional
    public ActionResultDto reject(UUID ownerId, UUID actionId) {
        AgentAction action = actionRepository.findForUpdate(actionId, ownerId)
                .orElseThrow(() -> new NotFoundException("Action not found"));

        if (action.getStatus() != ActionStatus.PENDING) {
            throw new BusinessRuleException("This action is no longer pending (" + action.getStatus() + ")");
        }

        action.decide(ActionStatus.CANCELLED, "Cancelled by the user", Instant.now());
        auditService.record(ownerId, action.getId(), AuditEventType.ACTION_CANCELLED, action.getType().name(), null);
        return ActionResultDto.of(action.getId(), action.getType(), ActionOutcome.CANCELLED, "Action cancelled");
    }

    @Transactional(readOnly = true)
    public List<ActionResultDto> pendingActions(UUID ownerId) {
        return actionRepository.findByOwnerIdAndStatusOrderByCreatedAtDesc(ownerId, ActionStatus.PENDING).stream()
                .filter(action -> !action.isExpired(Instant.now()))
                .map(action -> ActionResultDto.pending(action.getId(), action.getType(),
                        "Waiting for your confirmation", preview(ownerId, action)))
                .toList();
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void expirePendingActions() {
        int expired = actionRepository.expirePending(ActionStatus.PENDING, ActionStatus.EXPIRED, Instant.now());
        if (expired > 0) {
            log.debug("{} pending actions expired", expired);
        }
    }

    private ActionResultDto createPendingTransfer(UUID ownerId, UUID conversationId,
                                                  ValidatedAction.Transfer transfer) {
        Instant now = Instant.now();
        AgentAction action = actionRepository.save(AgentAction.builder()
                .ownerId(ownerId)
                .conversationId(conversationId)
                .type(ActionType.TRANSFER)
                .status(ActionStatus.PENDING)
                .fromAccountId(transfer.fromAccountId())
                .toIban(transfer.toIban())
                .amount(transfer.amount())
                .currency(transfer.currency())
                .description(transfer.description())
                .createdAt(now)
                .expiresAt(now.plus(properties.limits().pendingActionTtl()))
                .build());

        auditService.record(ownerId, action.getId(), AuditEventType.ACTION_PENDING, ActionType.TRANSFER.name(),
                "amount=%s %s, to=%s".formatted(transfer.amount(), transfer.currency(), transfer.toIban()));

        return ActionResultDto.pending(action.getId(), ActionType.TRANSFER,
                "Please confirm the transfer", preview(ownerId, action));
    }

    private TransferPreview preview(UUID ownerId, AgentAction action) {
        AccountDto from = accountService.getAccount(ownerId, action.getFromAccountId());
        return new TransferPreview(from.id(), from.name(), from.iban(), action.getToIban(),
                action.getAmount(), action.getCurrency(), action.getDescription(), action.getExpiresAt());
    }
}