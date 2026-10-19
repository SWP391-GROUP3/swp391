package com.swp391.kpi.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MUC LUC (so dong)
 *   17  field                        - cac cot cua bang (khop schema.sql)
 */
@Entity @Table(name = "cross_review_tasks")
@Getter @Setter @NoArgsConstructor
public class CrossReviewTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer batchId;
    private Integer articleId;
    private Integer reviewerId;
    private Integer originalScoringId;
    private BigDecimal score;
    private BigDecimal deviation;
    private LocalDateTime submittedAt;
    @Enumerated(EnumType.STRING)
    private CrossReviewTaskStatus status;
}
