package com.swp391.kpi.dto;

import com.swp391.kpi.entity.AuditLog;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MUC LUC (so dong)
 *   18  field                        - cac truong tra ve cho giao dien
 *   28  from()                       - doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class AuditLogResponse {

    private Integer id;
    private Integer actorId;
    private String actorDisplay;
    private String actionType;
    private String entityType;
    private Integer entityId;
    private String oldValue;
    private String newValue;
    private LocalDateTime createdAt;

    public static AuditLogResponse from(AuditLog entity) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(entity.getId());
        response.setActorId(entity.getActorId());
        response.setActorDisplay(entity.getActorId() == null
                ? "Hệ thống"
                : "Người dùng #" + entity.getActorId());
        response.setActionType(entity.getActionType());
        response.setEntityType(entity.getEntityType());
        response.setEntityId(entity.getEntityId());
        response.setOldValue(entity.getOldValue());
        response.setNewValue(entity.getNewValue());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
