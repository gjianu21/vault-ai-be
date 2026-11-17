package com.vaultai.be.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.vaultai.be.ai.*;
import com.vaultai.be.dto.response.AgentPromptDetailDto;
import com.vaultai.be.dto.response.AgentPromptSummaryDto;
import com.vaultai.be.dto.response.AgentResult;
import com.vaultai.be.model.entity.AgentPrompt;
import com.vaultai.be.model.enums.PromptStatus;
import com.vaultai.be.model.enums.ReplySource;
import com.vaultai.be.repository.AgentPromptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaultai.be.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Prompt -> save -> execute -> process -> fallback -> save result.
 * Not @Transactional on purpose: the CLI call takes seconds and must not hold a DB connection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final PromptBuilder promptBuilder;
    private final PromptExecutor executor;
    private final ResponseProcessor responseProcessor;
    private final FallbackReplyService fallback;
    private final AgentPromptRepository promptRepository;

    public AgentResult ask(UUID ownerId, UUID conversationId, String message, List<AgentMessage> history, String context) {
        String prompt = promptBuilder.build(context, history, message);

        AgentPrompt entry = promptRepository.save(AgentPrompt.builder()
                .ownerId(ownerId)
                .conversationId(conversationId)
                .userMessage(message)
                .prompt(prompt)
                .status(PromptStatus.PENDING)
                .createdAt(Instant.now())
                .build());

        return execute(entry, context);
    }

    /** Runs a saved prompt again, exactly as it was. The result is saved as a new entry. */
    public AgentResult replay(UUID ownerId, UUID promptId) {
        AgentPrompt original = promptRepository.findByIdAndOwnerId(promptId, ownerId)
                .orElseThrow(() -> new NotFoundException("Prompt not found"));

        AgentPrompt entry = promptRepository.save(AgentPrompt.builder()
                .ownerId(ownerId)
                .conversationId(original.getConversationId())
                .replayOfId(original.getId())
                .userMessage(original.getUserMessage())
                .prompt(original.getPrompt())
                .status(PromptStatus.PENDING)
                .createdAt(Instant.now())
                .build());

        return execute(entry, null);
    }

    @Transactional(readOnly = true)
    public List<AgentPromptSummaryDto> getPrompts(UUID ownerId, UUID conversationId) {
        List<AgentPrompt> prompts = conversationId == null
                ? promptRepository.findTop50ByOwnerIdOrderByCreatedAtDesc(ownerId)
                : promptRepository.findByOwnerIdAndConversationIdOrderByCreatedAtAsc(ownerId, conversationId);
        return prompts.stream().map(AgentPromptSummaryDto::from).toList();
    }

    @Transactional(readOnly = true)
    public AgentPromptDetailDto getPrompt(UUID ownerId, UUID promptId) {
        return promptRepository.findByIdAndOwnerId(promptId, ownerId)
                .map(AgentPromptDetailDto::from)
                .orElseThrow(() -> new NotFoundException("Prompt not found"));
    }

    private AgentResult execute(AgentPrompt entry, String context) {
        long start = System.nanoTime();
        CliResult result = null;
        try {
            result = executor.execute(entry.getPrompt());
            ResponseProcessor.ProcessedReply processed = responseProcessor.process(result);

            complete(entry, result, processed.text(), executor.source(), null, start);
            return new AgentResult(entry.getId(), processed.text(), executor.source(), processed.citedSources());
        } catch (RuntimeException ex) {
            log.warn("AI call failed for prompt {}, using fallback: {}", entry.getId(), ex.getMessage());

            String reply = fallback.reply(entry.getUserMessage(), context);
            complete(entry, result, reply, ReplySource.FALLBACK, ex.getMessage(), start);
            return new AgentResult(entry.getId(), reply, ReplySource.FALLBACK, List.of());
        }
    }

    private void complete(AgentPrompt entry, CliResult result, String reply, ReplySource source,
                          String failureReason, long startNanos) {
        entry.setRawOutput(rawOutput(result));
        entry.setExitCode(result == null ? null : result.exitCode());
        entry.setReply(reply);
        entry.setReplySource(source);
        entry.setStatus(source == ReplySource.FALLBACK ? PromptStatus.FALLBACK : PromptStatus.SUCCESS);
        entry.setFailureReason(truncate(failureReason, 1000));
        entry.setDurationMs((System.nanoTime() - startNanos) / 1_000_000);
        entry.setCompletedAt(Instant.now());
        promptRepository.save(entry);
    }

    private static String rawOutput(CliResult result) {
        if (result == null) {
            return null;
        }
        if (result.stderr() == null || result.stderr().isBlank()) {
            return result.stdout();
        }
        return result.stdout() + "\n--- stderr ---\n" + result.stderr();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}