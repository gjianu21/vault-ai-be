package com.vaultai.be.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vaultai.be.auth.CurrentUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Chat")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Send a message to the assistant (conversationId empty for a new conversation)")
    @PostMapping
    public ChatResponse send(@Valid @RequestBody ChatRequest request, @AuthenticationPrincipal OidcUser user) {
        return chatService.sendMessage(CurrentUser.id(user), request.conversationId(), request.message());
    }

    @Operation(summary = "The user's conversations, most recent first")
    @GetMapping("/conversations")
    public List<ConversationDto> conversations(@AuthenticationPrincipal OidcUser user) {
        return chatService.getConversations(CurrentUser.id(user));
    }

    @Operation(summary = "All messages of a conversation")
    @GetMapping("/conversations/{conversationId}/messages")
    public List<ChatMessageDto> messages(@PathVariable UUID conversationId, @AuthenticationPrincipal OidcUser user) {
        return chatService.getMessages(CurrentUser.id(user), conversationId);
    }
}