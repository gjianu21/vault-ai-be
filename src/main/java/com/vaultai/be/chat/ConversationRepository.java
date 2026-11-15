package com.vaultai.be.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Conversation> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}