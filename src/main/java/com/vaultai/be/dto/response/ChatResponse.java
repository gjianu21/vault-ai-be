package com.vaultai.be.dto.response;

import java.util.List;
import java.util.UUID;

import com.vaultai.be.model.enums.ReplySource;

public record ChatResponse(
        UUID conversationId,
        String reply,
        UUID promptId,
        ReplySource source,
        List<String> citedSources) {
}
