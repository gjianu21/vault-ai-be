package com.vaultai.be.agent;

import java.util.Map;

/**
 * An action suggested by the agent. Untrusted input: ActionValidator checks everything
 * (type, parameters, ownership, limits) before anything is executed.
 */
public record ProposedAction(String type, Map<String, Object> parameters) {
}