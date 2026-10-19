package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.RoyaltyPeriodDetailResponse;
import com.swp391.kpi.dto.RoyaltyPeriodResponse;
import com.swp391.kpi.service.RoyaltyPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API ky nhuan but: danh sach ky (UC-49), bang ke ky va chot ky (UC-50).
 * Xem: Thu ky va Ke toan. Chot ky: chi Thu ky (luat trong SecurityConfig).
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

    // GET /api/royalty-periods/{id}: bang ke ky (UC-50 buoc 1): bai da cham + tong tien tung nguoi
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoyaltyPeriodDetailResponse>> getPeriodDetail(@PathVariable Integer id) {
        RoyaltyPeriodDetailResponse data = royaltyPeriodService.getPeriodDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy bảng kê kỳ nhuận bút thành công", data));
    }

    // POST /api/royalty-periods/{id}/close: Thu ky chot ky OPEN -> CLOSED (UC-50)
    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<RoyaltyPeriodResponse>> closePeriod(
            @PathVariable Integer id,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());   // id Thu ky dang nhap, lay tu token
        RoyaltyPeriodResponse data = royaltyPeriodService.closePeriod(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Chốt kỳ nhuận bút thành công", data));
    }
}
