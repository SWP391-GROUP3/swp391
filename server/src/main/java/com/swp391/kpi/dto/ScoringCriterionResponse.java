package com.swp391.kpi.dto;

import java.math.BigDecimal;

public record ScoringCriterionResponse(Integer criterionId, String name, BigDecimal score, String comment) {}
