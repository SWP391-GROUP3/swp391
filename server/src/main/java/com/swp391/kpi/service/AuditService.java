package com.swp391.kpi.service;

import com.swp391.kpi.dto.AuditLogResponse;
import com.swp391.kpi.entity.AuditLog;
import com.swp391.kpi.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ghi va doc bang audit_logs (dung chung): moi thao tac quan trong ghi 1 dong ai - lam gi - gia tri
 * truoc / sau.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   30  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   33  log()                           ghi log
 *   53  getLogs()                       lay logs
 *  ---- Ham phu (private, chi dung trong file) ----
 *   76  emptyToNull()                   empty to null
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(
            Integer actorId,
            String actionType,
            String entityType,
            Integer entityId,
            String oldValue,
            String newValue
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorId(actorId);
        auditLog.setActionType(actionType);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogs(
            Integer actorId,
            String actionType,
            String entityType,
            Integer entityId,
            LocalDateTime fromTime,
            LocalDateTime toTime
    ) {
        List<AuditLog> logs = auditLogRepository.search(
                actorId,
                emptyToNull(actionType),
                emptyToNull(entityType),
                entityId,
                fromTime,
                toTime
        );
        List<AuditLogResponse> result = new ArrayList<>();
        for (AuditLog log : logs) {
            result.add(AuditLogResponse.from(log));
        }
        return result;
    }

    private String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
