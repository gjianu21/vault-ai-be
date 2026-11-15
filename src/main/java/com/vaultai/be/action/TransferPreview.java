package com.vaultai.be.action;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** What the user sees on the confirmation card. */
public record TransferPreview(
        UUID fromAccountId,
        String fromAccountName,
        String fromIban,
        String toIban,
        BigDecimal amount,
        String currency,
        String description,
        Instant expiresAt) {
}