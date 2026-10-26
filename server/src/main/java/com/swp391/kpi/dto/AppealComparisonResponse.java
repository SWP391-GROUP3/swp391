package com.swp391.kpi.dto;

import com.swp391.kpi.service.AppealScoreCalculator;
import java.math.BigDecimal;

public record AppealComparisonResponse(Integer appealId, String status, BigDecimal threshold,
        AppealBlindScorecardResponse original, AppealBlindScorecardResponse first,
        AppealBlindScorecardResponse second, AppealScoreCalculator.Result proposal,
        BigDecimal confirmedScore, String confirmedRule) {}
