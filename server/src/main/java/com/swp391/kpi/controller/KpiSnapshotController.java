package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.DepartmentKpiDashboardResponse;
import com.swp391.kpi.dto.KpiSnapshotResponse;
import com.swp391.kpi.dto.ReporterKpiDetailResponse;
import com.swp391.kpi.entity.KpiSnapshot;
import com.swp391.kpi.service.KpiAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller cung cap cac API cho Job tong hop KPI va bang theo doi KPI toan ban (UC-61).
 */
@RestController
@RequestMapping("/api/kpi-snapshots")
@RequiredArgsConstructor
public class KpiSnapshotController {

    private final KpiAggregationService kpiAggregationService;

    /**
     * Lay du lieu bang theo doi KPI ban bien tap (UC-61: View Department KPI Board).
     * GET /api/kpi-snapshots/dashboard?year=2026&month=9
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DepartmentKpiDashboardResponse>> getDepartmentDashboard(
            @RequestParam Short year,
            @RequestParam Byte month
    ) {
        DepartmentKpiDashboardResponse data = kpiAggregationService.getDepartmentDashboard(year, month);
        return ResponseEntity.ok(ApiResponse.success("Lấy dữ liệu bảng KPI ban biên tập thành công", data));
    }

    /**
     * Lay chi tiet hieu suat KPI cua mot phong vien theo chu ky thang (UC-62, UC-60).
     * GET /api/kpi-snapshots/reporters/{reporterId}/details?year=2026&month=9
     */
    @GetMapping("/reporters/{reporterId}/details")
    public ResponseEntity<ApiResponse<ReporterKpiDetailResponse>> getReporterKpiDetail(
            @PathVariable Integer reporterId,
            @RequestParam Short year,
            @RequestParam Byte month
    ) {
        ReporterKpiDetailResponse data = kpiAggregationService.getReporterKpiDetail(reporterId, year, month);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết KPI phóng viên thành công", data));
    }

    /**
     * Kich hoat job tong hop KPI tu dong theo chu ky thang (Automated Aggregation Job).
     * POST /api/kpi-snapshots/aggregate?year=2026&month=9
     */
    @PostMapping("/aggregate")
    public ResponseEntity<ApiResponse<List<KpiSnapshotResponse>>> triggerAggregation(
            @RequestParam Short year,
            @RequestParam Byte month
    ) {
        List<KpiSnapshot> snapshots = kpiAggregationService.aggregateMonthlyKpi(year, month);
        List<KpiSnapshotResponse> responseList = new ArrayList<>();
        for (KpiSnapshot s : snapshots) {
            responseList.add(KpiSnapshotResponse.from(s));
        }
        return ResponseEntity.ok(ApiResponse.success("Tổng hợp dữ liệu KPI chu kỳ thành công", responseList));
    }
}
