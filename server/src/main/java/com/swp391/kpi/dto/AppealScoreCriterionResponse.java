package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO tra ve cho giao dien. Dung o: AppealService.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class AppealScoreCriterionResponse {

    private Integer criterionId;
    private String criterionName;
    private BigDecimal score;
    private String comment;
}
