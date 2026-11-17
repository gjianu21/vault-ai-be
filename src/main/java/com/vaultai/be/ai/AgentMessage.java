package com.vaultai.be.ai;

/** One message from the conversation history. role: "user" or "assistant". */
public record AgentMessage(String role, String content) {
}