package com.vaultai.be.dto.request;

import jakarta.validation.constraints.NotNull;

public record SaveConversationRequest(@NotNull(message = "saved is required") Boolean saved) {
}