package com.swp391.kpi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Quyết định duyệt điểm của Trưởng ban biên tập cho một lần chấm.
 * Bảng: score_approvals
 */
@Entity
@Table(name = "score_approvals")
@Getter
@Setter
@NoArgsConstructor
public class ScoreApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scoring_attempt_id", nullable = false, unique = true)
    private ScoringAttempt scoringAttempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 10)
    private ApprovalDecision decision;

    // Bắt buộc nếu decision = 'RETURNED'
    @Column(name = "reason", length = 1000)
    private String reason;

    // FK -> users(id): Trưởng ban ra quyết định
    @Column(name = "decided_by", nullable = false)
    private Integer decidedBy;

    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt = LocalDateTime.now();

    // Thời điểm công bố điểm (khi decision = 'APPROVED')
    @Column(name = "announced_at")
    private LocalDateTime announcedAt;
}
