package com.vaultai.be.action;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface AgentActionRepository extends JpaRepository<AgentAction, UUID> {

    /** Locks the row, so the same action cannot be confirmed twice in parallel (double click). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AgentAction a where a.id = :id and a.ownerId = :ownerId")
    Optional<AgentAction> findForUpdate(@Param("id") UUID id, @Param("ownerId") UUID ownerId);

    List<AgentAction> findByOwnerIdAndStatusOrderByCreatedAtDesc(UUID ownerId, ActionStatus status);

    @Modifying
    @Query("""
            update AgentAction a
               set a.status = :expired, a.decidedAt = :now
             where a.status = :pending and a.expiresAt < :now
            """)
    int expirePending(@Param("pending") ActionStatus pending,
                      @Param("expired") ActionStatus expired,
                      @Param("now") Instant now);
}