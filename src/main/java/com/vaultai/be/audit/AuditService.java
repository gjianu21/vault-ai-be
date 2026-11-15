package com.vaultai.be.audit;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    /** Separate transaction: the audit entry is kept even if the main operation fails afterwards. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID ownerId, UUID actionId, AuditEventType eventType, String actionType, String details) {
        repository.save(AuditLog.builder()
                .ownerId(ownerId)
                .actionId(actionId)
                .eventType(eventType)
                .actionType(truncate(actionType, 40))
                .details(truncate(details, 1000))
                .createdAt(Instant.now())
                .build());
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}