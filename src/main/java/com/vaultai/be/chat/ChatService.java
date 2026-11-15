package com.vaultai.be.chat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.vaultai.be.account.AccountService;
import com.vaultai.be.action.ActionResultDto;
import com.vaultai.be.action.ActionService;
import com.vaultai.be.action.ActionType;
import com.vaultai.be.agent.AgentClient;
import com.vaultai.be.agent.AgentContext;
import com.vaultai.be.agent.AgentMessage;
import com.vaultai.be.agent.AgentRequest;
import com.vaultai.be.agent.AgentResponse;
import com.vaultai.be.common.NotFoundException;
import com.vaultai.be.config.AppProperties;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int TITLE_LENGTH = 60;

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final AccountService accountService;
    private final AgentClient agentClient;
    private final ActionService actionService;
    private final AppProperties properties;

    /**
     * Not @Transactional on purpose: the agent call can take several seconds
     * and must not keep a database connection (and locks) open.
     */
    public ChatResponse sendMessage(UUID ownerId, UUID conversationId, String text) {
        Conversation conversation = conversationId == null
                ? startConversation(ownerId, text)
                : conversationRepository.findByIdAndOwnerId(conversationId, ownerId)
                .orElseThrow(() -> new NotFoundException("Conversation not found"));

        List<AgentMessage> history = recentHistory(conversation.getId());
        saveMessage(conversation, MessageRole.USER, text, null);

        AgentResponse agentResponse = agentClient.send(
                new AgentRequest(conversation.getId(), text, history, buildContext(ownerId)));

        ActionResultDto action = agentResponse.action() == null
                ? null
                : actionService.handleProposal(ownerId, conversation.getId(), agentResponse.action());

        String reply = StringUtils.hasText(agentResponse.reply()) ? agentResponse.reply() : "Done.";
        saveMessage(conversation, MessageRole.ASSISTANT, reply, action == null ? null : action.actionId());

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        return new ChatResponse(conversation.getId(), reply, action);
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

    private List<AgentMessage> recentHistory(UUID conversationId) {
        List<ChatMessage> latest = messageRepository.findByConversation_IdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(0, properties.agent().historySize()));

        List<AgentMessage> history = new ArrayList<>(latest.stream()
                .map(message -> new AgentMessage(message.getRole().name().toLowerCase(Locale.ROOT),
                        message.getContent()))
                .toList());
        Collections.reverse(history); // oldest first
        return history;
    }

    private void saveMessage(Conversation conversation, MessageRole role, String content, UUID actionId) {
        messageRepository.save(ChatMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .actionId(actionId)
                .createdAt(Instant.now())
                .build());
    }

    private AgentContext buildContext(UUID ownerId) {
        List<AgentContext.AccountInfo> accounts = accountService.getAccounts(ownerId).stream()
                .map(account -> new AgentContext.AccountInfo(account.id().toString(), account.name(),
                        account.type().name(), account.currency()))
                .toList();

        List<AgentContext.ActionInfo> actions = Arrays.stream(ActionType.values())
                .map(type -> new AgentContext.ActionInfo(type.name(), type.description(), type.parameters(),
                        type.requiresConfirmation()))
                .toList();

        return new AgentContext(accounts, actions);
    }
}