package com.vaultai.be.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.vaultai.be.model.enums.PromptStatus;
import com.vaultai.be.model.enums.ReplySource;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Every prompt sent to the AI, with its raw output and the final reply. */
@Entity
@Table(name = "agent_prompt")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPrompt {

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

    /** Set when this prompt is a re-run of an older one. */
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "replay_of_id")
    private UUID replayOfId;

    @Lob
    @Column(name = "user_message", nullable = false)
    private String userMessage;

    @Lob
    @Column(name = "prompt", nullable = false)
    private String prompt;

    @Lob
    @Column(name = "raw_output")
    private String rawOutput;

    @Lob
    @Column(name = "reply")
    private String reply;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PromptStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reply_source", length = 20)
    private ReplySource replySource;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
