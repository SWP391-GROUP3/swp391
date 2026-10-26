package com.swp391.kpi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CrossReviewBatchResponse(Integer id, String batchName, LocalDate periodStart, LocalDate periodEnd,
        String status, Integer createdBy, LocalDateTime createdAt, List<Task> tasks) {
    public record Task(Integer id, Integer articleId, String title, Integer reviewerId, Integer originalScoringId,
            BigDecimal score, BigDecimal deviation, String status, LocalDateTime submittedAt) {}
}
