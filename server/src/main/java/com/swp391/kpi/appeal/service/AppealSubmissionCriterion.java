package com.swp391.kpi.appeal.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 1 tieu chi trong goi du lieu khieu nai: ma tieu chi, diem, trong so.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  field                           repository / service duoc inject
 */
@Getter
@AllArgsConstructor
public class AppealSubmissionCriterion {

    private Integer criterionId;
    private String criterionName;
    private BigDecimal score;
    private String comment;
}
