package com.swp391.kpi.appeal.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO tra ve cho giao dien. Dung o: AppealController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   20  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class AppealEligibilityResponse {

    private Integer articleId;
    private String articleTitle;
    private BigDecimal totalScore;
    private LocalDateTime announcedAt;
    private LocalDateTime appealDeadline;
    private List<AppealScoreCriterionResponse> criteria;
}
