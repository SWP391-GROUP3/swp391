package com.swp391.kpi.dto;

import com.swp391.kpi.entity.ScoringAttempt;
import java.time.LocalDateTime;

public record ScoringAppealAttemptResponse(Integer id, Integer articleId, Integer versionId,
        Integer assignmentId, Integer criteriaSetVersionId, Integer categoryId, Integer appealId,
        Integer reviewerId, Integer attemptNo, boolean crossDepartment, LocalDateTime submittedAt) {
    public static ScoringAppealAttemptResponse from(ScoringAttempt attempt) {
        return new ScoringAppealAttemptResponse(attempt.getId(), attempt.getArticleId(), attempt.getVersionId(),
                attempt.getAssignment().getId(), attempt.getCriteriaSetVersion().getId(), attempt.getCategoryId(),
                attempt.getAppealId(), attempt.getReviewerId(), attempt.getAttemptNo(),
                Boolean.TRUE.equals(attempt.getIsCrossDepartment()), attempt.getSubmittedAt());
    }
}
