package com.vaultai.be.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionDto(
        UUID id,
        UUID accountId,
        TransactionType type,
        BigDecimal amount,
        String currency,
        String description,
        String counterpartyName,
        String counterpartyIban,
        Instant bookedAt,
        BigDecimal balanceAfter) {

    public static TransactionDto from(AccountTransaction transaction) {
        return new TransactionDto(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getDescription(),
                transaction.getCounterpartyName(),
                transaction.getCounterpartyIban(),
                transaction.getBookedAt(),
                transaction.getBalanceAfter());
    }
}