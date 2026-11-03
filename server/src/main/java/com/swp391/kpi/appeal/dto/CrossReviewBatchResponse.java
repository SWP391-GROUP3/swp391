package com.swp391.kpi.appeal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO tra ve cho giao dien. Dung o: CrossReviewController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   16  Task()                          task
 */
public record CrossReviewBatchResponse(Integer id, String batchName, LocalDate periodStart, LocalDate periodEnd,
        String status, Integer createdBy, LocalDateTime createdAt, List<Task> tasks) {
    public record Task(Integer id, Integer articleId, String title, Integer reviewerId, Integer originalScoringId,
            BigDecimal score, BigDecimal deviation, String status, LocalDateTime submittedAt) {}
}
