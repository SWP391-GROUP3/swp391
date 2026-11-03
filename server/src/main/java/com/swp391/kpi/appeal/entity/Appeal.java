package com.swp391.kpi.appeal.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity anh xa bang appeals trong schema.sql. Chi dung trong service, khong tra thang ra API.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   31  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "appeals")
@Getter
@Setter
@NoArgsConstructor
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer articleId;
    private Integer scoringAttemptId;
    private Integer appellantId;
    private Integer ruleConfigId;

    @Enumerated(EnumType.STRING)
    private AppealStatus status;

    private LocalDateTime submittedAt;
    private LocalDateTime resolvedAt;
}
