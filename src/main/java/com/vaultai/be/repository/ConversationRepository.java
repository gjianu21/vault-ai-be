package com.vaultai.be.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.vaultai.be.model.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Conversation> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    @Query("""
            select c from Conversation c
            where c.ownerId = :ownerId and (c.closed = false or c.saved = true)
            order by c.updatedAt desc""")
    List<Conversation> findVisibleByOwnerId(@Param("ownerId") UUID ownerId);
}