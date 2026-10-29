package com.swp391.kpi.dto;

import com.swp391.kpi.entity.KpiSnapshotStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO dai dien cho 1 dong phong vien tren Dashboard KPI cua ban bien tap (UC-61).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class DepartmentKpiItemResponse {

    private Integer reporterId;
    private String reporterName;
    private String reporterEmail;

    // Chi tieu giao dau thang (UC-59)
    private Integer minPublished;
    private BigDecimal minAvgScore;

    // Ket qua thuc te (BR-KPI-02)
    private Integer actualArticles;
    private BigDecimal exactActualArticles; // So thap phan chinh xac vd: 4.60 bai
    private BigDecimal actualAvgScore;

    // Tien do hoan thanh
    private BigDecimal articleProgressPercent; // (actualArticles / minPublished) * 100

    // Ket qua danh gia nhi phan (BR-KPI-05: PASS / NOT_PASS)
    private String projectedStatus; // "PASS" hoac "NOT_PASS"

    // Trang thai snapshot (DRAFT hoac FINALIZED)
    private KpiSnapshotStatus snapshotStatus;
    private String notes;
}
