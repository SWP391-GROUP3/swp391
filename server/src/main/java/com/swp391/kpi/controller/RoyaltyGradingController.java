package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.RoyaltyPendingArticleResponse;
import com.swp391.kpi.service.RoyaltyGradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API cham nhuan but cua Thu ky toa soan (UC-46; UC-47, 48 lam o buoc sau).
 */
@RestController
@RequestMapping("/api/royalty-gradings")
@RequiredArgsConstructor
public class RoyaltyGradingController {

    private final RoyaltyGradingService royaltyGradingService;

    // GET /api/royalty-gradings/pending: bai da dang chua cham, kem han chi va canh bao
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<RoyaltyPendingArticleResponse>>> getPendingArticles() {
        List<RoyaltyPendingArticleResponse> data = royaltyGradingService.getPendingArticles();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài chờ chấm nhuận bút thành công", data));
    }
}
