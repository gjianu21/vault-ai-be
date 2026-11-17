package com.vaultai.be.dto.response;

import com.vaultai.be.model.enums.ReplySource;

import java.util.List;
import java.util.UUID;

public record AgentResult(UUID promptId, String reply, ReplySource source, List<String> citedSources) {
}