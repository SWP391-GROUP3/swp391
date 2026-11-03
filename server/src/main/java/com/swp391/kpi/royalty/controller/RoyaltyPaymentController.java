package com.swp391.kpi.royalty.controller;

import com.swp391.kpi.common.dto.ApiResponse;
import com.swp391.kpi.royalty.dto.RoyaltyMyPeriodResponse;
import com.swp391.kpi.royalty.dto.RoyaltyPaymentResponse;
import com.swp391.kpi.royalty.service.RoyaltyPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API phieu chi nhuan but: Ke toan xem phieu chi cua ky va ghi nhan da chi (UC-54),
 * nguoi dang nhap xem nhuan but cua chinh minh (UC-57). Quyen nam trong SecurityConfig.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   35  field                           service duoc inject
 *   39  getPaymentsOfPeriod()           lay payments cua period [GET /api/royalty-payments]
 *   47  recordPayment()                 ghi payment [POST /api/royalty-payments/{id}/pay]
 *   57  getMyRoyalties()                lay my royalties [GET /api/royalty-payments/me]
 */
@RestController
@RequestMapping("/api/royalty-payments")
@RequiredArgsConstructor
public class RoyaltyPaymentController {

    private final RoyaltyPaymentService royaltyPaymentService;

    // GET /api/royalty-payments?periodId=5: phieu chi cua tung nguoi trong ky (UC-54 buoc 1)
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoyaltyPaymentResponse>>> getPaymentsOfPeriod(
            @RequestParam Integer periodId) {
        List<RoyaltyPaymentResponse> data = royaltyPaymentService.getPaymentsOfPeriod(periodId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phiếu chi thành công", data));
    }

    // POST /api/royalty-payments/{id}/pay: Ke toan ghi nhan da chi 1 phieu (UC-54 buoc 2-3)
    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<RoyaltyPaymentResponse>> recordPayment(
            @PathVariable Integer id,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());   // id Ke toan dang nhap, lay tu token
        RoyaltyPaymentResponse data = royaltyPaymentService.recordPayment(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Ghi nhận chi nhuận bút thành công", data));
    }

    // GET /api/royalty-payments/me: nhuan but cua nguoi dang dang nhap theo tung ky (UC-57)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<RoyaltyMyPeriodResponse>>> getMyRoyalties(
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<RoyaltyMyPeriodResponse> data = royaltyPaymentService.getMyRoyalties(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy nhuận bút của bạn thành công", data));
    }
}
