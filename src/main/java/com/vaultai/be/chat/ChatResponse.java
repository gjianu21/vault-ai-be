package com.vaultai.be.chat;

import java.util.UUID;

/** The dashboard, suggestions and agent mode are added in plan step BE-9. */
public record ChatResponse(UUID conversationId, String reply) {
}
