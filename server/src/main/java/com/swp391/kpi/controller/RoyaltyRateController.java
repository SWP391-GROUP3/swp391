package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.RoyaltyRateCreateRequest;
import com.swp391.kpi.dto.RoyaltyRateResponse;
import com.swp391.kpi.service.RoyaltyRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API don gia nhuan but cho man Unit Price List (UC-55, Ke toan).
 */
@RestController
@RequestMapping("/api/royalty-rates")
@RequiredArgsConstructor
public class RoyaltyRateController {

    private final RoyaltyRateService royaltyRateService;

    // GET /api/royalty-rates: lich su don gia, moi nhat len dau
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoyaltyRateResponse>>> getAllRates() {
        List<RoyaltyRateResponse> data = royaltyRateService.getAllRates();
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đơn giá thành công", data));
    }

    // POST /api/royalty-rates: them don gia moi
    @PostMapping
    public ResponseEntity<ApiResponse<RoyaltyRateResponse>> createRate(
            @Valid @RequestBody RoyaltyRateCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());   // id nguoi dang nhap, lay tu token
        RoyaltyRateResponse data = royaltyRateService.createRate(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm đơn giá thành công", data));
    }
}
