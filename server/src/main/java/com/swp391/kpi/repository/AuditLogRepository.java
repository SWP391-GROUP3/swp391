package com.swp391.kpi.repository;

import com.swp391.kpi.entity.AuditLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository truy van bang cua entity ? (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   26  save()                          luu
 *   31  WHERE()                         hang so
 *   32  AND()                           hang so
 *   33  AND()                           hang so
 *   34  AND()                           hang so
 *   35  AND()                           hang so
 *   36  AND()                           hang so
 *   39  search()                        tim
 */
public interface AuditLogRepository extends Repository<AuditLog, Integer> {

    AuditLog save(AuditLog auditLog);

    @Query("""
            SELECT auditLog
            FROM AuditLog auditLog
            WHERE (:actorId IS NULL OR auditLog.actorId = :actorId)
              AND (:actionType IS NULL OR auditLog.actionType = :actionType)
              AND (:entityType IS NULL OR auditLog.entityType = :entityType)
              AND (:entityId IS NULL OR auditLog.entityId = :entityId)
              AND (:fromTime IS NULL OR auditLog.createdAt >= :fromTime)
              AND (:toTime IS NULL OR auditLog.createdAt <= :toTime)
            ORDER BY auditLog.createdAt DESC, auditLog.id DESC
            """)
    List<AuditLog> search(
            @Param("actorId") Integer actorId,
            @Param("actionType") String actionType,
            @Param("entityType") String entityType,
            @Param("entityId") Integer entityId,
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime
    );
}
