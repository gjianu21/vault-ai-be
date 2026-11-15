package com.vaultai.be.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaultai.be.common.NotFoundException;
import com.vaultai.be.config.AppProperties;

import lombok.RequiredArgsConstructor;

/** Read operations. Every method checks that the account belongs to the given owner. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private static final int DEFAULT_TRANSACTIONS_LIMIT = 20;

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository transactionRepository;
    private final AppProperties properties;

    public List<AccountDto> getAccounts(UUID ownerId) {
        return accountRepository.findByOwnerIdOrderByCreatedAtAsc(ownerId).stream()
                .map(AccountDto::from)
                .toList();
    }

    public AccountDto getAccount(UUID ownerId, UUID accountId) {
        return AccountDto.from(requireOwnedAccount(ownerId, accountId));
    }

    public List<TransactionDto> getTransactions(UUID ownerId, UUID accountId, Integer limit) {
        requireOwnedAccount(ownerId, accountId);
        int size = clampLimit(limit);
        return transactionRepository.findByAccount_IdOrderByBookedAtDesc(accountId, PageRequest.of(0, size))
                .stream()
                .map(TransactionDto::from)
                .toList();
    }

    /** "Not found" also when the account exists but belongs to someone else: we reveal nothing. */
    public Account requireOwnedAccount(UUID ownerId, UUID accountId) {
        return accountRepository.findByIdAndOwnerId(accountId, ownerId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }

    public BigDecimal outgoingAmountSince(UUID ownerId, Instant since) {
        BigDecimal total = transactionRepository.sumAmountByOwnerAndTypeSince(ownerId, TransactionType.DEBIT, since);
        return total == null ? BigDecimal.ZERO : total;
    }

    private int clampLimit(Integer limit) {
        int max = properties.limits().maxTransactionsPerQuery();
        if (limit == null || limit < 1) {
            return Math.min(DEFAULT_TRANSACTIONS_LIMIT, max);
        }
        return Math.min(limit, max);
    }
}