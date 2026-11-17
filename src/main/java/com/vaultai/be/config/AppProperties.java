package com.vaultai.be.config;

import java.time.Duration;
import java.util.List;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String frontendUrl,
        @NotBlank String internalApiKey,
        @Valid @NotNull Agent agent,
        @Valid @NotNull DemoData demoData) {

    public record Agent(
            @NotEmpty List<String> cliCommand,
            @NotNull Duration timeout,
            @Min(1) @Max(50) int historySize,
            @Min(2000) @Max(30000) int maxPromptChars,
            @Min(1) @Max(5) int maxConcurrent) {
    }

    public record DemoData(
            boolean enabled,
            @NotBlank String currency) {
    }
}