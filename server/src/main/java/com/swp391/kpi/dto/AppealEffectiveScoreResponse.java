package com.swp391.kpi.dto;

import java.math.BigDecimal;

public record AppealEffectiveScoreResponse(Integer articleId, String appealStatus, BigDecimal effectiveScore) {}
