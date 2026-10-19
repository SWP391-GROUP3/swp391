package com.swp391.kpi.service;

import com.swp391.kpi.dto.AuditLogResponse;
import com.swp391.kpi.entity.AuditLog;
import com.swp391.kpi.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        auditService = new AuditService(auditLogRepository);
    }

    @Test
    void logOnlyInsertsNewAuditEntry() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog saved = invocation.getArgument(0);
            saved.setId(12);
            return saved;
        });

        auditService.log(1, "CREATE", "appeals", 7, null, "{\"status\":\"SUBMITTED\"}");

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void getLogsMapsSystemActorAndKeepsNewestOrderFromRepository() {
        AuditLog log = new AuditLog();
        log.setId(5);
        log.setActorId(null);
        log.setActionType("SYNC");
        log.setEntityType("appeals");
        log.setEntityId(3);
        log.setNewValue("{\"status\":\"RESOLVED\"}");
        log.setCreatedAt(LocalDateTime.of(2026, 10, 14, 8, 0));
        when(auditLogRepository.search(null, null, null, null, null, null))
                .thenReturn(List.of(log));

        List<AuditLogResponse> result = auditService.getLogs(null, null, null, null, null, null);

        assertEquals(1, result.size());
        assertEquals("Hệ thống", result.get(0).getActorDisplay());
        assertEquals("SYNC", result.get(0).getActionType());
    }
}
