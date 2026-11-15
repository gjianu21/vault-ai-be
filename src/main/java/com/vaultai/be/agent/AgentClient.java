package com.vaultai.be.agent;

/**
 * The only contract between the backend and the AI agent.
 * MockAgentClient on the "mock" profile, RestAgentClient otherwise.
 * When the format of the internal agent is known, only RestAgentClient changes.
 */
public interface AgentClient {

    AgentResponse send(AgentRequest request);
}