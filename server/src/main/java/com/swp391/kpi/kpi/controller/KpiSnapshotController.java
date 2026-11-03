package com.swp391.kpi.kpi.controller;

import com.swp391.kpi.common.dto.ApiResponse;
import com.swp391.kpi.kpi.dto.DepartmentKpiDashboardResponse;
import com.swp391.kpi.kpi.dto.KpiFinalizeRequest;
import com.swp391.kpi.kpi.dto.KpiFinalizeResponse;
import com.swp391.kpi.kpi.dto.KpiSnapshotResponse;
import com.swp391.kpi.kpi.dto.ReporterKpiDetailResponse;
import com.swp391.kpi.kpi.service.KpiAggregationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller cung cap cac API cho Job tong hop KPI va bang theo doi KPI toan ban (UC-61).
 * Quyen theo vai tro nam trong SecurityConfig; phong vien chi xem KPI cua minh (kiem trong service).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   41  field                           service duoc inject
 *   48  getDepartmentDashboard()        Lay du lieu bang theo doi KPI ban bien tap (UC-61: View Department KPI Boa… [GET /api/kpi-snapshots/dashboard]
 *   61  getReporterKpiDetail()          Lay chi tiet hieu suat KPI cua mot phong vien theo ch… [GET /api/kpi-snapshots/reporters/{reporterId}/details]
 *   77  triggerAggregation()            Kich hoat job tong hop KPI tu dong theo chu ky thang (Automated Aggregati… [POST /api/kpi-snapshots/aggregate]
 *   90  finalizeMonthlyKpi()            Chot ky KPI thang va luu snapshot bat bien (UC-66: Finalize Monthly KPI) [POST /api/kpi-snapshots/finalize]
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
            @RequestParam Byte month,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());   // nguoi dang xem, lay tu token
        ReporterKpiDetailResponse data = kpiAggregationService.getReporterKpiDetail(reporterId, year, month, currentUserId);
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
        List<KpiSnapshotResponse> data = kpiAggregationService.aggregateMonthlyKpi(year, month);
        return ResponseEntity.ok(ApiResponse.success("Tổng hợp dữ liệu KPI chu kỳ thành công", data));
    }

    /**
     * Chot ky KPI thang va luu snapshot bat bien (UC-66: Finalize Monthly KPI).
     * POST /api/kpi-snapshots/finalize
     */
    @PostMapping("/finalize")
    public ResponseEntity<ApiResponse<KpiFinalizeResponse>> finalizeMonthlyKpi(
            @Valid @RequestBody KpiFinalizeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        KpiFinalizeResponse response = kpiAggregationService.finalizeMonthlyKpi(
                request.getYear(), request.getMonth(), currentUserId, request.getReporterNotes()
        );
        return ResponseEntity.ok(ApiResponse.success("Chốt kỳ KPI tháng thành công", response));
    }
}
