package com.swp391.kpi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity anh xa bang cross_review_batches trong schema.sql. Chi dung trong service, khong tra thang ra
 * API.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  field                           cac cot cua bang (khop schema.sql)
 */
@Entity @Table(name = "cross_review_batches")
@Getter @Setter @NoArgsConstructor
public class CrossReviewBatch {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String batchName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer createdBy;
    @Enumerated(EnumType.STRING)
    private CrossReviewBatchStatus status;
    private LocalDateTime createdAt;
}
