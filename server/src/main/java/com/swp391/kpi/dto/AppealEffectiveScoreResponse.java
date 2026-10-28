package com.swp391.kpi.dto;

import java.math.BigDecimal;

/**
 * DTO tra ve cho giao dien. Dung o: AppealReviewController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   11  record                          cac truong khai bao trong ngoac cua record
 */
public record AppealEffectiveScoreResponse(Integer articleId, String appealStatus, BigDecimal effectiveScore) {}
