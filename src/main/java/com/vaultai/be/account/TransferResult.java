package com.vaultai.be.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResult(
        UUID transactionId,
        UUID fromAccountId,
        String toIban,
        BigDecimal amount,
        String currency,
        BigDecimal newBalance,
        Instant bookedAt) {
}