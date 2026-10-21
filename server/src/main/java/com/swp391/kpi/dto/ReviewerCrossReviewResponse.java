package com.swp391.kpi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReviewerCrossReviewResponse(Integer id, Integer articleId, BigDecimal score,
        BigDecimal deviation, String status, LocalDateTime submittedAt) {}
