package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.CategoryChangeRequest;
import com.swp391.kpi.dto.ScoringAttemptResponse;
import com.swp391.kpi.service.ScoringAttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

// Controller xu ly mo phieu cham, snapshot tieu chi va doi the loai (UC-22, UC-23, UC-30)
@RestController
@RequestMapping("/api/scoring-attempts")
@RequiredArgsConstructor
public class ScoringAttemptController {

    private final ScoringAttemptService scoringAttemptService;

    // Mo phieu cham cho bai viet va snapshot bo tieu chi ACTIVE (UC-22, BR-SC-06)
    @PostMapping("/article/{articleId}/open")
    public ResponseEntity<ApiResponse<ScoringAttemptResponse>> openScoringAttempt(
            @PathVariable Integer articleId,
            @AuthenticationPrincipal Jwt jwt) {
        Integer reviewerId = Integer.valueOf(jwt.getSubject());
        ScoringAttemptResponse response = scoringAttemptService.openScoringAttempt(articleId, reviewerId);
        return ResponseEntity.ok(ApiResponse.success("Mở phiếu chấm thành công", response));
    }

    // BTV doi the loai bai viet truoc khi cham (UC-23, BR-SC-05)
    @PostMapping("/article/{articleId}/change-category")
    public ResponseEntity<ApiResponse<ScoringAttemptResponse>> changeCategory(
            @PathVariable Integer articleId,
            @Valid @RequestBody CategoryChangeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer editorId = Integer.valueOf(jwt.getSubject());
        ScoringAttemptResponse response = scoringAttemptService.changeCategory(articleId, request.getNewCategoryId(), editorId);
        return ResponseEntity.ok(ApiResponse.success("Đổi thể loại bài viết thành công", response));
    }

    // Lay lan cham gan nhat cua mot bai viet (UC-30, UC-34)
    @GetMapping("/article/{articleId}/latest")
    public ResponseEntity<ApiResponse<ScoringAttemptResponse>> getLatestAttempt(
            @PathVariable Integer articleId) {
        ScoringAttemptResponse response = scoringAttemptService.getLatestAttemptByArticle(articleId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phiếu chấm thành công", response));
    }
}
