package com.vaultai.be.config;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String frontendUrl,
        @NotBlank String internalApiKey,
        @Valid @NotNull Agent agent,
        @Valid @NotNull Limits limits,
        @Valid @NotNull DemoData demoData) {

    public record Agent(
            String baseUrl,
            String chatPath,
            String apiToken,
            @NotNull Duration timeout,
            @Min(1) @Max(50) int historySize) {
    }

    public record Limits(
            @NotNull @Positive BigDecimal maxTransferAmount,
            @NotNull @Positive BigDecimal dailyTransferLimit,
            @NotNull Duration pendingActionTtl,
            @Min(1) @Max(100) int maxTransactionsPerQuery) {
    }

    public record DemoData(
            boolean enabled,
            @NotBlank String currency) {
    }
}