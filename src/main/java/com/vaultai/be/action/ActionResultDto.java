package com.vaultai.be.action;

import java.util.UUID;

/**
 * Result of an action, shown in the UI next to the agent's reply.
 * data: AccountDto, a list of TransactionDto, TransferPreview or TransferResult, depending on the action.
 */
public record ActionResultDto(
        UUID actionId,
        ActionType type,
        ActionOutcome outcome,
        String message,
        Object data) {

    public static ActionResultDto completed(UUID actionId, ActionType type, String message, Object data) {
        return new ActionResultDto(actionId, type, ActionOutcome.COMPLETED, message, data);
    }

    public static ActionResultDto pending(UUID actionId, ActionType type, String message, Object data) {
        return new ActionResultDto(actionId, type, ActionOutcome.PENDING_CONFIRMATION, message, data);
    }

    public static ActionResultDto rejected(UUID actionId, ActionType type, String reason) {
        return new ActionResultDto(actionId, type, ActionOutcome.REJECTED, reason, null);
    }

    public static ActionResultDto of(UUID actionId, ActionType type, ActionOutcome outcome, String message) {
        return new ActionResultDto(actionId, type, outcome, message, null);
    }
}