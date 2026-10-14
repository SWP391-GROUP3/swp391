package com.swp391.kpi.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class AppealSubmissionCriterion {

    private Integer criterionId;
    private String criterionName;
    private BigDecimal score;
    private String comment;
}
