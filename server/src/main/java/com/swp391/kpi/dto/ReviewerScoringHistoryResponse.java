package com.swp391.kpi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO tra ve cho giao dien. Dung o: ReviewerProfileController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   12  record                          cac truong khai bao trong ngoac cua record
 */
public record ReviewerScoringHistoryResponse(Integer attemptId, Integer articleId, String articleTitle,
        String attemptType, BigDecimal totalScore, LocalDateTime submittedAt) {}
