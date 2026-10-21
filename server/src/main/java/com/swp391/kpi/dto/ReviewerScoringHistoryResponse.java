package com.swp391.kpi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReviewerScoringHistoryResponse(Integer attemptId, Integer articleId, String articleTitle,
        String attemptType, BigDecimal totalScore, LocalDateTime submittedAt) {}
