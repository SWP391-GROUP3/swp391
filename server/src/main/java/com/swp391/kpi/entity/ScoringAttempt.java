package com.swp391.kpi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một lần chấm bài viết (lần đầu, chấm lại do Trưởng ban trả về, hoặc chấm khiếu nại).
 * Bảng: scoring_attempts
 *
 * MUC LUC (so dong)
 *   27  field                        - cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "scoring_attempts")
@Getter
@Setter
@NoArgsConstructor
public class ScoringAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK -> articles(id)
    @Column(name = "article_id", nullable = false)
    private Integer articleId;

    // FK -> article_versions(id)
    @Column(name = "version_id", nullable = false)
    private Integer versionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    // FK -> appeals(id) (chỉ có khi attempt_type = 'APPEAL')
    @Column(name = "appeal_id")
    private Integer appealId;

    // FK -> users(id): Người chấm bài
    @Column(name = "reviewer_id", nullable = false)
    private Integer reviewerId;

    @Column(name = "attempt_no", nullable = false)
    private Integer attemptNo = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "attempt_type", nullable = false, length = 10)
    private AttemptType attemptType = AttemptType.INITIAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criteria_set_version_id", nullable = false)
    private CriteriaSetVersion criteriaSetVersion;

    // FK -> genres(id): thể loại áp dụng tại thời điểm chấm
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    // Tổng điểm (0.00 đến 100.00), null khi đang chấm nháp
    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", length = 10)
    private ScoringDecision decision;

    @Column(name = "decision_note", length = 1000)
    private String decisionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replaces_attempt_id")
    private ScoringAttempt replacesAttempt;

    @Column(name = "is_cross_department", nullable = false)
    private Boolean isCrossDepartment = false;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt = LocalDateTime.now();

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}
