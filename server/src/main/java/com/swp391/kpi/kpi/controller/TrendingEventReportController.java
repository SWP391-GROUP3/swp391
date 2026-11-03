package com.swp391.kpi.kpi.controller;

import com.swp391.kpi.common.dto.ApiResponse;
import com.swp391.kpi.kpi.dto.TrendingEventReportResponse;
import com.swp391.kpi.kpi.service.TrendingEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controller cung cap API bao cao su kien xu huong va phat hien dot bien (UC-65).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  field                           service xu ly
 *   28  getTrendingEventsReport()       GET /api/kpi-reports/trending-events
 */
@RestController
@RequestMapping("/api/kpi-reports")
@RequiredArgsConstructor
public class TrendingEventReportController {

    private final TrendingEventService trendingEventService;

    /**
     * Lay bao cao phan tich su kien xu huong theo ngay moc (UC-65).
     * GET /api/kpi-reports/trending-events?anchorDate=2026-09-16
     */
    @GetMapping("/trending-events")
    public ResponseEntity<ApiResponse<TrendingEventReportResponse>> getTrendingEventsReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate anchorDate
    ) {
        TrendingEventReportResponse response = trendingEventService.getTrendingEventsReport(anchorDate);
        return ResponseEntity.ok(ApiResponse.success("Lấy báo cáo sự kiện xu hướng thành công", response));
    }
}
