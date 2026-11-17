package com.vaultai.be.dto.response;

import com.vaultai.be.model.entity.Conversation;

import java.time.Instant;
import java.util.UUID;

public record ConversationDto(UUID id, String title, boolean saved, boolean closed,
                              Instant createdAt, Instant updatedAt) {

    public static ConversationDto from(Conversation conversation) {
        return new ConversationDto(conversation.getId(), conversation.getTitle(), conversation.isSaved(),
                conversation.isClosed(), conversation.getCreatedAt(), conversation.getUpdatedAt());
    }
}