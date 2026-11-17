package com.vaultai.be.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vaultai.be.model.entity.AgentPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentPromptRepository extends JpaRepository<AgentPrompt, UUID> {

    Optional<AgentPrompt> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<AgentPrompt> findTop50ByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    List<AgentPrompt> findByOwnerIdAndConversationIdOrderByCreatedAtAsc(UUID ownerId, UUID conversationId);
}
