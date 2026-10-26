package com.swp391.kpi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
