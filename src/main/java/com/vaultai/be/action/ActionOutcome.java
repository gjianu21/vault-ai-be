package com.vaultai.be.action;

/** What the UI receives after an action was handled. */
public enum ActionOutcome {
    COMPLETED,
    PENDING_CONFIRMATION,
    REJECTED,
    CANCELLED,
    EXPIRED
}