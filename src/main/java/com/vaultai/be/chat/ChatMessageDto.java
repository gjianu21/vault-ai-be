package com.vaultai.be.chat;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageDto(UUID id, MessageRole role, String content, UUID actionId, Instant createdAt) {

    public static ChatMessageDto from(ChatMessage message) {
        return new ChatMessageDto(message.getId(), message.getRole(), message.getContent(),
                message.getActionId(), message.getCreatedAt());
    }
}