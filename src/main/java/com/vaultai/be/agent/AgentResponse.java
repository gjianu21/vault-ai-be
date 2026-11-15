package com.vaultai.be.agent;

/** The text reply for the user, plus an optional action (null if there is none). */
public record AgentResponse(String reply, ProposedAction action) {
}