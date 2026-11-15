package com.vaultai.be.chat;

import java.time.Instant;
import java.util.UUID;

public record ConversationDto(UUID id, String title, Instant createdAt, Instant updatedAt) {

    public static ConversationDto from(Conversation conversation) {
        return new ConversationDto(conversation.getId(), conversation.getTitle(),
                conversation.getCreatedAt(), conversation.getUpdatedAt());
    }
}