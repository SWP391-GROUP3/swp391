package com.swp391.kpi.assessment.dto;

import java.math.BigDecimal;

/**
 * DTO tra ve cho giao dien. Dung o: ScoringReadService.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   11  record                          cac truong khai bao trong ngoac cua record
 */
public record ScoringCriterionResponse(Integer criterionId, String name, BigDecimal score, String comment) {}
