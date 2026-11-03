package com.swp391.kpi.kpi.controller;

import com.swp391.kpi.common.dto.ApiResponse;
import com.swp391.kpi.kpi.dto.GenrePopularityReportResponse;
import com.swp391.kpi.kpi.service.GenreReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cap API bao cao muc do quan tam the loai & chi phi nhuan but (UC-64).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  field                           service xu ly
 *   28  getGenrePopularityReport()      GET /api/kpi-reports/genres
 */
@RestController
@RequestMapping("/api/kpi-reports")
@RequiredArgsConstructor
public class GenreReportController {

    private final GenreReportService genreReportService;

    /**
     * Lay bao cao phan tich the loai theo nam va thang (UC-64).
     * GET /api/kpi-reports/genres?year=2026&month=9
     */
    @GetMapping("/genres")
    public ResponseEntity<ApiResponse<GenrePopularityReportResponse>> getGenrePopularityReport(
            @RequestParam(required = false) Short year,
            @RequestParam(required = false) Byte month
    ) {
        GenrePopularityReportResponse response = genreReportService.getGenrePopularityReport(year, month);
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo mức độ quan tâm thể loại thành công", response));
    }
}
