package com.vaultai.be.action;

import java.util.Locale;
import java.util.Optional;

/** The whitelist of actions the agent may ask for. Anything else is rejected. */
public enum ActionType {

    GET_BALANCE(false,
            "Show the balance of one of the user's accounts",
            "accountId"),
    GET_TRANSACTIONS(false,
            "List the most recent transactions of one of the user's accounts",
            "accountId, limit (optional, max 50)"),
    TRANSFER(true,
            "Prepare a money transfer from one of the user's accounts to an IBAN. The user must confirm it.",
            "fromAccountId, toIban, amount, description (optional)");

    private final boolean requiresConfirmation;
    private final String description;
    private final String parameters;

    ActionType(boolean requiresConfirmation, String description, String parameters) {
        this.requiresConfirmation = requiresConfirmation;
        this.description = description;
        this.parameters = parameters;
    }

    public boolean requiresConfirmation() {
        return requiresConfirmation;
    }

    public String description() {
        return description;
    }

    public String parameters() {
        return parameters;
    }

    public static Optional<ActionType> parse(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}