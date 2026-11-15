package com.vaultai.be.chat;

import java.util.UUID;

import com.vaultai.be.action.ActionResultDto;

/** action: null when the agent did not ask for any action. */
public record ChatResponse(UUID conversationId, String reply, ActionResultDto action) {
}