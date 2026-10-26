package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.RoyaltyGradingCreateRequest;
import com.swp391.kpi.dto.RoyaltyGradingResponse;
import com.swp391.kpi.dto.RoyaltyPendingArticleResponse;
import com.swp391.kpi.service.RoyaltyGradingService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API cham nhuan but cua Thu ky toa soan (UC-46, 47, 48).
 * SecurityConfig chi cho vai tro EDITORIAL_SECRETARY goi /api/royalty-gradings/**.
 *
 * MUC LUC (so dong)
 *   38  field                        - service duoc inject
 *   42  getPendingArticles()         - GET /api/royalty-gradings/pending: bai da dang chua cham, kem han chi v…
 *   50  previewGrading()             - GET /api/royalty-gradings/preview?articleId=5
 *   58  completeGrading()            - POST /api/royalty-gradings body { "articleId": 5 }
 */
@RestController
@RequestMapping("/api/royalty-gradings")
@RequiredArgsConstructor
public class RoyaltyGradingController {

    private final RoyaltyGradingService royaltyGradingService;

    // GET /api/royalty-gradings/pending: bai da dang chua cham, kem han chi va canh bao (UC-46)
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<RoyaltyPendingArticleResponse>>> getPendingArticles() {
        List<RoyaltyPendingArticleResponse> data = royaltyGradingService.getPendingArticles();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài chờ chấm nhuận bút thành công", data));
    }

    // GET /api/royalty-gradings/preview?articleId=5
    // Xem truoc he so va tien, chua luu (UC-47 buoc 1-3). Tinh chat bai lay theo PV khai.
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<RoyaltyGradingResponse>> previewGrading(@RequestParam Integer articleId) {
        RoyaltyGradingResponse data = royaltyGradingService.previewGrading(articleId);
        return ResponseEntity.ok(ApiResponse.success("Tính nhuận bút xem trước thành công", data));
    }

    // POST /api/royalty-gradings  body { "articleId": 5 }
    // Hoan tat cham: luu phieu, anh/video, dong tien vao ky dang OPEN (UC-47 buoc 4, UC-48)
    @PostMapping
    public ResponseEntity<ApiResponse<RoyaltyGradingResponse>> completeGrading(
            @Valid @RequestBody RoyaltyGradingCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());   // id Thu ky dang nhap, lay tu token
        RoyaltyGradingResponse data = royaltyGradingService.completeGrading(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Chấm nhuận bút thành công", data));
    }
}
