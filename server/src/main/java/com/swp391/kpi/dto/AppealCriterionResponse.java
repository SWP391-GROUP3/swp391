package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO tra ve cho giao dien. Dung o: AppealManagementService, AppealService.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   16  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class AppealCriterionResponse {

    private Integer criterionId;
    private String criterionName;
    private String reason;
}
