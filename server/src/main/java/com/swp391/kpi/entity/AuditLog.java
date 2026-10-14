package com.swp391.kpi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

@Entity
@org.hibernate.annotations.Immutable
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK toi users(id). De Integer de khong phu thuoc entity cua luong admin.
    private Integer actorId;

    private String actionType;

    private String entityType;

    private Integer entityId;

    @Lob
    @Nationalized
    private String oldValue;

    @Lob
    @Nationalized
    private String newValue;

    private LocalDateTime createdAt;
}
