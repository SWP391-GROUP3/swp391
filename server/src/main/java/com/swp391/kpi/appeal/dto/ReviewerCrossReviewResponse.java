package com.swp391.kpi.appeal.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO tra ve cho giao dien. Dung o: ReviewerProfileService.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   12  record                          cac truong khai bao trong ngoac cua record
 */
public record ReviewerCrossReviewResponse(Integer id, Integer articleId, BigDecimal score,
        BigDecimal deviation, String status, LocalDateTime submittedAt) {}
