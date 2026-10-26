package com.swp391.kpi.dto;

import com.swp391.kpi.entity.AttemptType;
import com.swp391.kpi.entity.ScoringAttempt;
import com.swp391.kpi.entity.ScoringDecision;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ScoringAttemptResponse {
    private Integer id;
    private Integer articleId;
    private String articleTitle;
    private String articleContent;
    private Integer versionId;
    private Integer assignmentId;
    private Integer reviewerId;
    private Integer attemptNo;
    private AttemptType attemptType;
    private Integer criteriaSetVersionId;
    private Integer categoryId;
    private BigDecimal totalScore;
    private ScoringDecision decision;
    private String decisionNote;
    private LocalDateTime openedAt;
    private LocalDateTime submittedAt;
    private List<ScoringAttemptDetailResponse> details;

    public static ScoringAttemptResponse from(ScoringAttempt attempt,
                                              List<ScoringAttemptDetailResponse> details,
                                              String articleTitle,
                                              String articleContent) {
        ScoringAttemptResponse res = new ScoringAttemptResponse();
        res.setId(attempt.getId());
        res.setArticleId(attempt.getArticleId());
        res.setArticleTitle(articleTitle);
        res.setArticleContent(articleContent);
        res.setVersionId(attempt.getVersionId());
        res.setAssignmentId(attempt.getAssignment() != null ? attempt.getAssignment().getId() : null);
        res.setReviewerId(attempt.getReviewerId());
        res.setAttemptNo(attempt.getAttemptNo());
        res.setAttemptType(attempt.getAttemptType());
        res.setCriteriaSetVersionId(attempt.getCriteriaSetVersion() != null ? attempt.getCriteriaSetVersion().getId() : null);
        res.setCategoryId(attempt.getCategoryId());
        res.setTotalScore(attempt.getTotalScore());
        res.setDecision(attempt.getDecision());
        res.setDecisionNote(attempt.getDecisionNote());
        res.setOpenedAt(attempt.getOpenedAt());
        res.setSubmittedAt(attempt.getSubmittedAt());
        res.setDetails(details);
        return res;
    }
}
