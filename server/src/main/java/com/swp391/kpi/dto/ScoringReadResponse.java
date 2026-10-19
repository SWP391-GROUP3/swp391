package com.swp391.kpi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MUC LUC (so dong)
 *   13  ScoringReadResponse()
 */
public record ScoringReadResponse(Integer id, Integer articleId, Integer reviewerId, String attemptType,
                                  BigDecimal totalScore, LocalDateTime submittedAt, LocalDateTime announcedAt,
                                  Integer appealId) {
    public ScoringReadResponse(Integer id, Integer articleId, Integer reviewerId, String attemptType,
            BigDecimal totalScore, LocalDateTime submittedAt, LocalDateTime announcedAt) {
        this(id, articleId, reviewerId, attemptType, totalScore, submittedAt, announcedAt, null);
    }
}
