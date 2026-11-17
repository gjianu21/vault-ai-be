package com.vaultai.be.controller;

import java.util.List;
import java.util.UUID;

import com.vaultai.be.dto.request.ChatRequest;
import com.vaultai.be.dto.request.SaveConversationRequest;
import com.vaultai.be.dto.response.ChatMessageDto;
import com.vaultai.be.dto.response.ChatResponse;
import com.vaultai.be.dto.response.ConversationDto;
import com.vaultai.be.service.ChatService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import com.vaultai.be.security.CurrentUser;

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

    @Operation(summary = "Save or unsave a conversation")
    @PutMapping("/conversations/{conversationId}/saved")
    public ConversationDto setSaved(@PathVariable UUID conversationId,
                                    @Valid @RequestBody SaveConversationRequest request,
                                    @AuthenticationPrincipal OidcUser user) {
        return chatService.setSaved(CurrentUser.id(user), conversationId, request.saved());
    }

    @Operation(summary = "Close a conversation (saved conversations stay in the Saved list)")
    @PostMapping("/conversations/{conversationId}/close")
    public ConversationDto close(@PathVariable UUID conversationId, @AuthenticationPrincipal OidcUser user) {
        return chatService.close(CurrentUser.id(user), conversationId);
    }
}