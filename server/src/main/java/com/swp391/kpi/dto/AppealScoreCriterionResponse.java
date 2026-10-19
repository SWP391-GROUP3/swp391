package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * MUC LUC (so dong)
 *   16  field                        - cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class AppealScoreCriterionResponse {

    private Integer criterionId;
    private String criterionName;
    private BigDecimal score;
    private String comment;
}
