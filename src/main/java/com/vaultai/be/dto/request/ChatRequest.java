package com.vaultai.be.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** conversationId: null for a new conversation. */
public record ChatRequest(
        UUID conversationId,
        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message is too long (max 2000 characters)")
        String message) {
}