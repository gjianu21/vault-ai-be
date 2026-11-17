package com.vaultai.be.chat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaultai.be.common.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int TITLE_LENGTH = 60;

    /** Temporary reply until the dashboard agent is wired in (plan step BE-9). */
    private static final String PLACEHOLDER_REPLY =
            "The cockpit assistant is being set up. Dashboards will be available soon.";

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;

    /**
     * Not @Transactional on purpose: the agent call (added in plan step BE-9) can take
     * several seconds and must not keep a database connection (and locks) open.
     */
    public ChatResponse sendMessage(UUID ownerId, UUID conversationId, String text) {
        Conversation conversation = conversationId == null
                ? startConversation(ownerId, text)
                : conversationRepository.findByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        saveMessage(conversation, MessageRole.USER, text);

        String reply = PLACEHOLDER_REPLY;
        saveMessage(conversation, MessageRole.ASSISTANT, reply);

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        return new ChatResponse(conversation.getId(), reply);
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversations(UUID ownerId) {
        return conversationRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId).stream()
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
