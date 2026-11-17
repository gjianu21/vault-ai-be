package com.vaultai.be.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.vaultai.be.ai.AgentMessage;
import com.vaultai.be.dto.response.AgentResult;
import com.vaultai.be.config.AppProperties;
import com.vaultai.be.dto.response.ChatMessageDto;
import com.vaultai.be.dto.response.ChatResponse;
import com.vaultai.be.dto.response.ConversationDto;
import com.vaultai.be.model.entity.ChatMessage;
import com.vaultai.be.model.entity.Conversation;
import com.vaultai.be.model.enums.MessageRole;
import com.vaultai.be.repository.ChatMessageRepository;
import com.vaultai.be.repository.ConversationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaultai.be.exception.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int TITLE_LENGTH = 60;

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final AgentService agentService;
    private final AppProperties properties;

    /**
     * Not @Transactional on purpose: the agent call (added in plan step BE-9) can take
     * several seconds and must not keep a database connection (and locks) open.
     */
    public ChatResponse sendMessage(UUID ownerId, UUID conversationId, String text) {
        Conversation conversation = conversationId == null
                ? startConversation(ownerId, text)
                : conversationRepository.findByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        conversation.setClosed(false);

        List<AgentMessage> history = loadHistory(conversation.getId());
        saveMessage(conversation, MessageRole.USER, text);

        String context = null; // the customer context comes with the JARVIS data
        AgentResult result = agentService.ask(ownerId, conversation.getId(), text, history, context);

        saveMessage(conversation, MessageRole.ASSISTANT, result.reply());

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        return new ChatResponse(conversation.getId(), result.reply(), result.promptId(),
                result.source(), result.citedSources());
    }

    private List<AgentMessage> loadHistory(UUID conversationId) {
        List<ChatMessage> latest = new ArrayList<>(messageRepository.findByConversation_IdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(0, properties.agent().historySize())));
        Collections.reverse(latest);
        return latest.stream()
                .map(message -> new AgentMessage(
                        message.getRole() == MessageRole.USER ? "user" : "assistant", message.getContent()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversations(UUID ownerId) {
        return conversationRepository.findVisibleByOwnerId(ownerId).stream()
                .map(ConversationDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessages(UUID ownerId, UUID conversationId) {
        conversationRepository.findByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        return messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId).stream()
                .map(ChatMessageDto::from)
                .toList();
    }

    @Transactional
    public ConversationDto setSaved(UUID ownerId, UUID conversationId, boolean saved) {
        Conversation conversation = conversationRepository.findByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        conversation.setSaved(saved);
        return ConversationDto.from(conversation);
    }

    @Transactional
    public ConversationDto close(UUID ownerId, UUID conversationId) {
        Conversation conversation = conversationRepository.findByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));
        conversation.setClosed(true);
        return ConversationDto.from(conversation);
    }

    private Conversation startConversation(UUID ownerId, String firstMessage) {
        String title = firstMessage.strip();
        if (title.length() > TITLE_LENGTH) {
            title = title.substring(0, TITLE_LENGTH) + "...";
        }
        Instant now = Instant.now();
        return conversationRepository.save(Conversation.builder()
                .ownerId(ownerId)
                .title(title)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private void saveMessage(Conversation conversation, MessageRole role, String content) {
        messageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .createdAt(Instant.now())
                .build());
    }
}
