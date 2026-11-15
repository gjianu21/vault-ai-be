package com.vaultai.be.agent;

import java.util.List;
import java.util.UUID;

public record AgentRequest(
        UUID conversationId,
        String message,
        List<AgentMessage> history,
        AgentContext context) {
}