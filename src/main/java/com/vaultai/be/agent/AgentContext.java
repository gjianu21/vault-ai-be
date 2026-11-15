package com.vaultai.be.agent;

import java.util.List;

/**
 * The minimum the agent needs to know. No balances, no IBANs, no tokens:
 * if the agent needs data, it asks for an action and the backend executes it.
 */
public record AgentContext(
        List<AccountInfo> accounts,
        List<ActionInfo> supportedActions) {

    public record AccountInfo(String id, String name, String type, String currency) {
    }

    public record ActionInfo(String type, String description, String parameters, boolean requiresConfirmation) {
    }
}