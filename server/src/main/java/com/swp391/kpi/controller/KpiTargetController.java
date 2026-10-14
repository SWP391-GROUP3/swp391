package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.KpiBenchmarkConfigDto;
import com.swp391.kpi.dto.KpiTargetCreateRequest;
import com.swp391.kpi.dto.KpiTargetResponse;
import com.swp391.kpi.dto.KpiTargetUpdateRequest;
import com.swp391.kpi.service.KpiTargetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller cung cấp REST API cho chức năng cấu hình định mức & giao chỉ tiêu tháng (UC-58, UC-59).
 */
@RestController
@RequestMapping("/api/kpi-targets")
@RequiredArgsConstructor
public class KpiTargetController {

    private final KpiTargetService kpiTargetService;

    /**
     * Lấy danh sách định mức chuẩn theo ngạch phóng viên (UC-58).
     * GET /api/kpi-targets/benchmarks
     */
    @GetMapping("/benchmarks")
    public ResponseEntity<ApiResponse<List<KpiBenchmarkConfigDto>>> getBenchmarks() {
        List<KpiBenchmarkConfigDto> data = kpiTargetService.getDefaultBenchmarks();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách định mức chuẩn thành công", data));
    }

    /**
     * Cập nhật danh sách định mức chuẩn theo ngạch phóng viên (UC-58).
     * PUT /api/kpi-targets/benchmarks
     */
    @PutMapping("/benchmarks")
    public ResponseEntity<ApiResponse<List<KpiBenchmarkConfigDto>>> updateBenchmarks(
            @RequestBody List<KpiBenchmarkConfigDto> benchmarks
    ) {
        List<KpiBenchmarkConfigDto> data = kpiTargetService.updateBenchmarks(benchmarks);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật định mức chuẩn thành công", data));
    }

    /**
     * Lấy danh sách chỉ tiêu theo kỳ (năm và tháng) (UC-59).
     * GET /api/kpi-targets?year=2026&month=9
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<KpiTargetResponse>>> getTargets(
            @RequestParam Short year,
            @RequestParam Byte month
    ) {
        List<KpiTargetResponse> data = kpiTargetService.getTargetsByPeriod(year, month);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chỉ tiêu KPI thành công", data));
    }

    /**
     * Lấy chi tiết một chỉ tiêu theo ID.
     * GET /api/kpi-targets/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<KpiTargetResponse>> getTargetById(@PathVariable Integer id) {
        KpiTargetResponse data = kpiTargetService.getTargetById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết chỉ tiêu KPI thành công", data));
    }

    /**
     * Giao chỉ tiêu tháng mới cho phóng viên (UC-59).
     * POST /api/kpi-targets
     */
    @PostMapping
    public ResponseEntity<ApiResponse<KpiTargetResponse>> createTarget(
            @Valid @RequestBody KpiTargetCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());   // id nguoi dang nhap, lay tu token
        KpiTargetResponse data = kpiTargetService.createTarget(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Giao chỉ tiêu KPI tháng thành công", data));
    }

    /**
     * Điều chỉnh chỉ tiêu đã giao (UC-59 Nhánh ngoại lệ A2).
     * PUT /api/kpi-targets/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<KpiTargetResponse>> updateTarget(
            @PathVariable Integer id,
            @Valid @RequestBody KpiTargetUpdateRequest request
    ) {
        KpiTargetResponse data = kpiTargetService.updateTarget(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chỉ tiêu KPI thành công", data));
    }

    /**
     * Xoá chỉ tiêu KPI.
     * DELETE /api/kpi-targets/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTarget(@PathVariable Integer id) {
        kpiTargetService.deleteTarget(id);
        return ResponseEntity.ok(ApiResponse.success("Xoá chỉ tiêu KPI thành công", null));
    }
}
