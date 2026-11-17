package com.vaultai.be.repository;

import java.util.List;
import java.util.UUID;

import com.vaultai.be.model.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByConversation_IdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    List<ChatMessage> findByConversation_IdOrderByCreatedAtAsc(UUID conversationId);
}