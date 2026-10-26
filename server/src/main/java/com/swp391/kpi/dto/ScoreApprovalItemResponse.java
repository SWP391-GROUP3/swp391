package com.swp391.kpi.dto;

import com.swp391.kpi.entity.AttemptType;
import com.swp391.kpi.entity.ScoringAttempt;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreApprovalItemResponse {

    private Integer articleId;
    private String articleTitle;
    private Integer scoringAttemptId;
    private Integer attemptNo;
    private AttemptType attemptType;
    private Integer reviewerId;
    private String reviewerName;
    private Integer categoryId;
    private BigDecimal totalScore;
    private String decision;
    private String decisionNote;
    private LocalDateTime submittedAt;

    public static ScoreApprovalItemResponse from(ScoringAttempt attempt, String articleTitle, String reviewerName) {
        return new ScoreApprovalItemResponse(
                attempt.getArticleId(),
                articleTitle,
                attempt.getId(),
                attempt.getAttemptNo(),
                attempt.getAttemptType(),
                attempt.getReviewerId(),
                reviewerName,
                attempt.getCategoryId(),
                attempt.getTotalScore(),
                attempt.getDecision() != null ? attempt.getDecision().name() : null,
                attempt.getDecisionNote(),
                attempt.getSubmittedAt()
        );
    }
}
