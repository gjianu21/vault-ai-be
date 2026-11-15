package com.vaultai.be.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaultai.be.common.BusinessRuleException;
import com.vaultai.be.common.NotFoundException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

/**
 * Moves money. Called only by ActionService, after the user has confirmed.
 * Accounts are locked (SELECT ... FOR UPDATE) and re-read, so two transfers
 * at the same time cannot both spend the same money.
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final EntityManager entityManager;

    @Transactional
    public TransferResult transfer(UUID ownerId, UUID fromAccountId, String toIban,
                                   BigDecimal amount, String description) {
        Optional<UUID> internalTargetId = accountRepository.findIdByIban(toIban);

        if (internalTargetId.isPresent() && internalTargetId.get().equals(fromAccountId)) {
            throw new BusinessRuleException("Cannot transfer to the same account");
        }

        // always lock in the same order (by id) to avoid deadlocks between opposite transfers
        Stream.concat(Stream.of(fromAccountId), internalTargetId.stream())
                .sorted()
                .forEach(this::lockAccount);

        Account from = entityManager.find(Account.class, fromAccountId);
        if (from == null || !from.getOwnerId().equals(ownerId)) {
            throw new NotFoundException("Account not found");
        }
        Account to = internalTargetId.map(id -> entityManager.find(Account.class, id)).orElse(null);

        if (to != null && !to.getCurrency().equals(from.getCurrency())) {
            throw new BusinessRuleException("Currency conversion is not supported");
        }

        Instant now = Instant.now();
        from.debit(amount);
        AccountTransaction debit = transactionRepository.save(AccountTransaction.builder()
                .account(from)
                .type(TransactionType.DEBIT)
                .amount(amount)
                .currency(from.getCurrency())
                .description(description)
                .counterpartyName(to != null ? to.getName() : null)
                .counterpartyIban(toIban)
                .bookedAt(now)
                .balanceAfter(from.getBalance())
                .build());

        if (to != null) {
            to.credit(amount);
            transactionRepository.save(AccountTransaction.builder()
                    .account(to)
                    .type(TransactionType.CREDIT)
                    .amount(amount)
                    .currency(to.getCurrency())
                    .description(description)
                    .counterpartyName(from.getName())
                    .counterpartyIban(from.getIban())
                    .bookedAt(now)
                    .balanceAfter(to.getBalance())
                    .build());
        }

        return new TransferResult(debit.getId(), from.getId(), toIban, amount,
                from.getCurrency(), from.getBalance(), now);
    }

    /** SELECT ... FOR UPDATE and refresh, so we work with the latest balance. */
    private void lockAccount(UUID accountId) {
        Account account = entityManager.find(Account.class, accountId);
        if (account == null) {
            throw new NotFoundException("Account not found");
        }
        entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE);
    }
}