package com.swp391.kpi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO tra ve cho giao dien. Dung o: AppealReviewController, CrossReviewController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   16  Criterion()                     criterion
 */
public record AppealBlindScorecardResponse(Integer id, Integer articleId, Integer versionId,
        Integer categoryId, Integer criteriaSetVersionId, String title, String content,
        BigDecimal totalScore, LocalDateTime submittedAt, List<Criterion> criteria) {
    public record Criterion(Integer criterionId, String name, BigDecimal weight, BigDecimal score, String comment) {}
}
