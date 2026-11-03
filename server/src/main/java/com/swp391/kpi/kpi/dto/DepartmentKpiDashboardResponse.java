package com.swp391.kpi.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * DTO tong the tra ve cho man hinh Department KPI Board (UC-61).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class DepartmentKpiDashboardResponse {

    private Short year;
    private Byte month;
    private Integer totalReporters;
    private Integer passCount;
    private Integer notPassCount;
    private boolean isFinalized; // true neu tat ca da chot FINALIZED
    private List<DepartmentKpiItemResponse> items;
}
