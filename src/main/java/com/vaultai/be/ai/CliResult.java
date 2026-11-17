package com.vaultai.be.ai;

public record CliResult(int exitCode, String stdout, String stderr, long durationMs) {
}
