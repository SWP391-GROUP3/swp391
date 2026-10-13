package com.swp391.kpi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Điểm chi tiết cho từng tiêu chí trong một lần chấm.
 * Bảng: scoring_attempt_details
 */
@Entity
@Table(name = "scoring_attempt_details")
@Getter
@Setter
@NoArgsConstructor
public class ScoringAttemptDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scoring_attempt_id", nullable = false)
    private ScoringAttempt scoringAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criterion_id", nullable = false)
    private Criteria criterion;

    // Điểm số từ 0.00 đến 100.00
    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    // Nhận xét (bắt buộc nếu điểm < 50 theo BR-QA-08)
    @Column(name = "comment", length = 1000)
    private String comment;
}
