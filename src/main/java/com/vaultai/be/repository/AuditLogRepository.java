package com.vaultai.be.repository;

import java.util.UUID;

import com.vaultai.be.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
}