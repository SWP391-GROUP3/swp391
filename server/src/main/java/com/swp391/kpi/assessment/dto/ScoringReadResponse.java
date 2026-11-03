package com.swp391.kpi.assessment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO tra ve cho giao dien. Dung o: ReviewerDeviationRefreshService, ReviewerProfileService,
 * RoyaltyGradingService.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   16  ScoringReadResponse()           ham khoi tao
 */
public record ScoringReadResponse(Integer id, Integer articleId, Integer reviewerId, String attemptType,
                                  BigDecimal totalScore, LocalDateTime submittedAt, LocalDateTime announcedAt,
                                  Integer appealId) {
    public ScoringReadResponse(Integer id, Integer articleId, Integer reviewerId, String attemptType,
            BigDecimal totalScore, LocalDateTime submittedAt, LocalDateTime announcedAt) {
        this(id, articleId, reviewerId, attemptType, totalScore, submittedAt, announcedAt, null);
    }
}
