package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * MUC LUC (so dong)
 *   14  field                        - cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class AppealCriterionResponse {

    private Integer criterionId;
    private String criterionName;
    private String reason;
}
