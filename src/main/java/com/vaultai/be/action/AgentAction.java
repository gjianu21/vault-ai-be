package com.vaultai.be.action;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** An action waiting for the user's confirmation (today: transfers). */
@Entity
@Table(name = "agent_action")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentAction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "conversation_id")
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private ActionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ActionStatus status;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "from_account_id")
    private UUID fromAccountId;

    @Column(name = "to_iban", length = 34)
    private String toIban;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "description", length = 140)
    private String description;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "result_message", length = 500)
    private String resultMessage;

    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "result_transaction_id")
    private UUID resultTransactionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public ValidatedAction.Transfer toTransfer() {
        return new ValidatedAction.Transfer(fromAccountId, toIban, amount, currency, description);
    }

    public void decide(ActionStatus newStatus, String message, Instant now) {
        this.status = newStatus;
        this.resultMessage = message;
        this.decidedAt = now;
    }
}