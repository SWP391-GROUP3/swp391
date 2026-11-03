package com.swp391.kpi.appeal.dto;

import java.math.BigDecimal;

/**
 * Diem hieu luc cua 1 bai kem lan cham dang dung (BR-QC-10). Dung o: AppealEffectiveScoreService,
 * RoyaltyGradingService, KpiAggregationService, GenreReportService.
 * attemptId = lan cham S0 da duyet, hoac lan cham S1/S2 ma Truong ban chot khi khieu nai RESOLVED.
 * fromAppeal = true khi diem lay tu ket qua khieu nai.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   14  record                          cac truong khai bao trong ngoac cua record
 */
public record AppealEffectiveAttemptResponse(Integer attemptId, BigDecimal score, boolean fromAppeal) {}
