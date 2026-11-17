package com.vaultai.be.ai;

import java.util.List;
import java.util.regex.Pattern;

import com.vaultai.be.exception.AgentUnavailableException;
import org.springframework.stereotype.Component;

/** Turns the raw CLI output into the reply shown to the user, or rejects it. */
@Component
public class ResponseProcessor {

    private static final Pattern ANSI_CODES = Pattern.compile("\\u001B\\[[0-9;?]*[ -/]*[@-~]");
    private static final Pattern SOURCE_ID = Pattern.compile("\\[((?:CRM|PRD|CMP)-[A-Z0-9-]+)]");
    private static final int MAX_REPLY_CHARS = 8000;

    public ProcessedReply process(CliResult result) {
        if (result.exitCode() != 0) {
            throw new AgentUnavailableException(
                    "Copilot CLI exited with code " + result.exitCode() + ": " + abbreviate(result.stderr()));
        }

        String text = ANSI_CODES.matcher(result.stdout()).replaceAll("")
                .replace("\r\n", "\n")
                .strip();

        if (text.isBlank()) {
            throw new AgentUnavailableException("Copilot CLI returned an empty response");
        }
        if (text.length() > MAX_REPLY_CHARS) {
            text = text.substring(0, MAX_REPLY_CHARS) + "\n...";
        }

        List<String> citedSources = SOURCE_ID.matcher(text).results()
                .map(match -> match.group(1))
                .distinct()
                .toList();

        return new ProcessedReply(text, citedSources);
    }

    private static String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "(no error output)";
        }
        String stripped = value.strip();
        return stripped.length() <= 300 ? stripped : stripped.substring(0, 300) + "...";
    }

    public record ProcessedReply(String text, List<String> citedSources) {
    }
}