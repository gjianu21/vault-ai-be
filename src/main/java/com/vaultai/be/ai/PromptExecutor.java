package com.vaultai.be.ai;

import com.vaultai.be.model.enums.ReplySource;

/**
 * Runs a finished prompt and returns the raw result.
 * Throws AgentUnavailableException when there is no result at all (timeout, cannot start, busy).
 */
public interface PromptExecutor {

    CliResult execute(String prompt);

    default ReplySource source() {
        return ReplySource.AI;
    }
}
