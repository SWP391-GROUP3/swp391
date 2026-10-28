package com.swp391.kpi.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO tra ve cho giao dien. Dung o: AppealManagementController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   12  record                          cac truong khai bao trong ngoac cua record
 */
public record AppealManagementResponse(Integer id, Integer articleId, String articleTitle,
        Integer sectionId, Integer appellantId, String appellantName, String status,
        LocalDateTime submittedAt, LocalDateTime appealDeadline, boolean deadlineExpired,
        ScoringAppealAttemptResponse currentAttempt, List<AppealCriterionResponse> criteria) {}
