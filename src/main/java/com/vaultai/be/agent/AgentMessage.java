package com.vaultai.be.agent;

/** One message from the conversation history. role: "user" or "assistant". */
public record AgentMessage(String role, String content) {
}