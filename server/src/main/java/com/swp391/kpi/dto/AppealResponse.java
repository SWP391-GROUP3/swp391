package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO tra ve cho giao dien. Dung o: AppealController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class AppealResponse {

    private Integer id;
    private Integer articleId;
    private String articleTitle;
    private Integer scoringAttemptId;
    private String status;
    private LocalDateTime submittedAt;
    private List<AppealCriterionResponse> criteria;
    private java.math.BigDecimal effectiveScore;
    private LocalDateTime resolvedAt;
}
