package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AppealScoreCriterionResponse {

    private Integer criterionId;
    private String criterionName;
    private BigDecimal score;
    private String comment;
}
