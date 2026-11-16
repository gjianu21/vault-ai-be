package com.vaultai.be.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String frontendUrl,
        @NotBlank String internalApiKey,
        @Valid @NotNull Agent agent,
        @Valid @NotNull DemoData demoData) {

    public record Agent(
            String baseUrl,
            String chatPath,
            String apiToken,
            @NotNull Duration timeout,
            @Min(1) @Max(50) int historySize) {
    }

    public record DemoData(
            boolean enabled,
            @NotBlank String currency) {
    }
}