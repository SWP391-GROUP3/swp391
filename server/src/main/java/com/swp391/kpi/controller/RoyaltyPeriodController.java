package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.RoyaltyPeriodResponse;
import com.swp391.kpi.service.RoyaltyPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API ky nhuan but cho man Royalty Period List (UC-49, Thu ky va Ke toan).
 */
@RestController
@RequestMapping("/api/royalty-periods")
@RequiredArgsConstructor
public class RoyaltyPeriodController {

    private final RoyaltyPeriodService royaltyPeriodService;

    // GET /api/royalty-periods: cac ky moi nhat len dau, kem tong tien va so bai da cham
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoyaltyPeriodResponse>>> getAllPeriods() {
        List<RoyaltyPeriodResponse> data = royaltyPeriodService.getAllPeriods();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách kỳ nhuận bút thành công", data));
    }
}
