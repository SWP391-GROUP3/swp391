package com.swp391.kpi.royalty.controller;

import com.swp391.kpi.common.response.ApiResponse;
import com.swp391.kpi.royalty.dto.RoyaltyPeriodResponse;
import com.swp391.kpi.royalty.service.RoyaltyPeriodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/royalty-periods")
@RequiredArgsConstructor
public class RoyaltyPeriodController {

    private final RoyaltyPeriodService periodService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoyaltyPeriodResponse>>> getAll() {
        List<RoyaltyPeriodResponse> periods = periodService.getAll();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách kỳ nhuận bút thành công", periods));
    }
}
