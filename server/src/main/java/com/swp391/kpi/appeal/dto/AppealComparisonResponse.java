package com.swp391.kpi.appeal.dto;

import com.swp391.kpi.appeal.service.AppealScoreCalculator;
import java.math.BigDecimal;

/**
 * DTO tra ve cho giao dien. Dung o: AppealReviewController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   12  record                          cac truong khai bao trong ngoac cua record
 */
public record AppealComparisonResponse(Integer appealId, String status, BigDecimal threshold,
        AppealBlindScorecardResponse original, AppealBlindScorecardResponse first,
        AppealBlindScorecardResponse second, AppealScoreCalculator.Result proposal,
        BigDecimal confirmedScore, String confirmedRule) {}
