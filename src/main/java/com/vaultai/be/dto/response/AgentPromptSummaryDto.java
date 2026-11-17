package com.vaultai.be.dto.response;

import com.vaultai.be.model.entity.AgentPrompt;
import com.vaultai.be.model.enums.PromptStatus;
import com.vaultai.be.model.enums.ReplySource;

import java.time.Instant;
import java.util.UUID;

public record AgentPromptSummaryDto(
        UUID id,
        UUID conversationId,
        UUID replayOfId,
        String userMessage,
        PromptStatus status,
        ReplySource source,
        Long durationMs,
        Instant createdAt) {

    public static AgentPromptSummaryDto from(AgentPrompt prompt) {
        return new AgentPromptSummaryDto(prompt.getId(), prompt.getConversationId(), prompt.getReplayOfId(),
                prompt.getUserMessage(), prompt.getStatus(), prompt.getReplySource(),
                prompt.getDurationMs(), prompt.getCreatedAt());
    }
}