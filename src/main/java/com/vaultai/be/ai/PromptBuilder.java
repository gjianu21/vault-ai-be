package com.vaultai.be.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.vaultai.be.config.AppProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromptBuilder {

    public static final String NEW_MESSAGE_HEADER = "## New message from the employee";

    private static final String INSTRUCTIONS = """
            You are JARVIS, an AI assistant for relationship managers at a bank.
            You help the employee prepare for customer meetings.
            Rules:
            - Use ONLY the information in the Customer data section. If something is not there, say that you do not have that information.
            - After each fact, cite the record id in square brackets, for example [PRD-0007].
            - Detected events are already verified by the bank's systems. Base your recommendations on them.
            - Be concise. Use short paragraphs or bullet points.
            - End with one proactive suggestion or question for the employee.
            - Do not use any tools, do not read or write files and do not run commands. Answer with text only.
            """;

    private final AppProperties properties;

    /** Drops the oldest history messages until the prompt fits in max-prompt-chars. */
    public String build(String context, List<AgentMessage> history, String message) {
        int maxChars = properties.agent().maxPromptChars();
        List<AgentMessage> kept = new ArrayList<>(history);

        String prompt = render(context, kept, message);
        while (prompt.length() > maxChars && !kept.isEmpty()) {
            kept.removeFirst();
            prompt = render(context, kept, message);
        }
        return prompt;
    }

    private String render(String context, List<AgentMessage> history, String message) {
        StringBuilder prompt = new StringBuilder(INSTRUCTIONS);

        prompt.append("\n## Customer data\n")
                .append(StringUtils.hasText(context) ? context : "No customer data was provided for this question.");

        prompt.append("\n\n## Conversation so far\n");
        if (history.isEmpty()) {
            prompt.append("(this is the first message)\n");
        }
        for (AgentMessage item : history) {
            prompt.append("user".equals(item.role()) ? "Employee: " : "JARVIS: ")
                    .append(item.content())
                    .append('\n');
        }

        prompt.append('\n').append(NEW_MESSAGE_HEADER).append('\n').append(message.strip());
        return prompt.toString();
    }
}