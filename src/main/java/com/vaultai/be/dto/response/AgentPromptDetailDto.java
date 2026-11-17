package com.vaultai.be.dto.response;

import com.vaultai.be.model.entity.AgentPrompt;
import com.vaultai.be.model.enums.PromptStatus;
import com.vaultai.be.model.enums.ReplySource;

import java.time.Instant;
import java.util.UUID;

public record AgentPromptDetailDto(
        UUID id,
        UUID conversationId,
        UUID replayOfId,
        String userMessage,
        String prompt,
        String rawOutput,
        String reply,
        PromptStatus status,
        ReplySource source,
        Integer exitCode,
        Long durationMs,
        String failureReason,
        Instant createdAt,
        Instant completedAt) {

    public static AgentPromptDetailDto from(AgentPrompt p) {
        return new AgentPromptDetailDto(p.getId(), p.getConversationId(), p.getReplayOfId(), p.getUserMessage(),
                p.getPrompt(), p.getRawOutput(), p.getReply(), p.getStatus(), p.getReplySource(),
                p.getExitCode(), p.getDurationMs(), p.getFailureReason(), p.getCreatedAt(), p.getCompletedAt());
    }
}