package com.vaultai.be.action;

/** Lifecycle of an action that needs confirmation, as stored in agent_action. */
public enum ActionStatus {
    PENDING,
    EXECUTED,
    REJECTED,
    CANCELLED,
    EXPIRED
}