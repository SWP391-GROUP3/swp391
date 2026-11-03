package com.swp391.kpi.royalty.controller;

import com.swp391.kpi.common.dto.ApiResponse;
import com.swp391.kpi.royalty.dto.RoyaltyPeriodDetailResponse;
import com.swp391.kpi.royalty.dto.RoyaltyPeriodResponse;
import com.swp391.kpi.royalty.dto.RoyaltyPeriodReturnRequest;
import com.swp391.kpi.royalty.service.RoyaltyPeriodService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API ky nhuan but: danh sach ky (UC-49), bang ke ky va chot ky (UC-50), tra ky (UC-52), khoa ky (UC-53).
 * Xem: Thu ky va Ke toan. Chot ky: chi Thu ky. Tra / khoa ky: chi Ke toan (luat trong SecurityConfig).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   39  field                           service duoc inject
 *   43  getAllPeriods()                 lay tat ca periods [GET /api/royalty-periods]
 *   50  getPeriodDetail()               lay period detail [GET /api/royalty-periods/{id}]
 *   57  closePeriod()                   chot period [POST /api/royalty-periods/{id}/close]
 *   67  returnPeriod()                  tra ve period [POST /api/royalty-periods/{id}/return]
 *   78  lockPeriod()                    khoa period [POST /api/royalty-periods/{id}/lock]
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

    // POST /api/royalty-periods/{id}/return: Ke toan tra ky CLOSED ve OPEN kem ly do (UC-52)
    @PostMapping("/{id}/return")
    public ResponseEntity<ApiResponse<RoyaltyPeriodResponse>> returnPeriod(
            @PathVariable Integer id,
            @Valid @RequestBody RoyaltyPeriodReturnRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());   // id Ke toan dang nhap, lay tu token
        RoyaltyPeriodResponse data = royaltyPeriodService.returnPeriod(id, request.getNote(), currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Trả lại kỳ nhuận bút thành công", data));
    }

    // POST /api/royalty-periods/{id}/lock: Ke toan khoa ky CLOSED -> LOCKED, tao phieu chi (UC-53)
    @PostMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<RoyaltyPeriodResponse>> lockPeriod(
            @PathVariable Integer id,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());   // id Ke toan dang nhap, lay tu token
        RoyaltyPeriodResponse data = royaltyPeriodService.lockPeriod(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Khoá kỳ nhuận bút thành công", data));
    }
}
