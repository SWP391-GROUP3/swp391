package com.swp391.kpi.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AppealManagementResponse(Integer id, Integer articleId, String articleTitle,
        Integer sectionId, Integer appellantId, String appellantName, String status,
        LocalDateTime submittedAt, LocalDateTime appealDeadline, boolean deadlineExpired,
        ScoringAppealAttemptResponse currentAttempt, List<AppealCriterionResponse> criteria) {}
