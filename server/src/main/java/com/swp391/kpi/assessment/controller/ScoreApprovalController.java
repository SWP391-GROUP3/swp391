package com.swp391.kpi.assessment.controller;

import com.swp391.kpi.assessment.dto.PublishingItemResponse;
import com.swp391.kpi.assessment.dto.PublishingResponse;
import com.swp391.kpi.assessment.dto.ScoreApprovalDecisionRequest;
import com.swp391.kpi.assessment.dto.ScoreApprovalItemResponse;
import com.swp391.kpi.assessment.dto.ScoreApprovalResponse;
import com.swp391.kpi.assessment.service.ScoreApprovalService;
import com.swp391.kpi.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xu ly cac API phe duyet diem danh gia va duyet xuat ban cua Truong ban (UC-29, UC-31, UC-32, UC-33).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   35  field                           service duoc inject
 *   41  getPendingApprovals()           Lay danh sach bai viet cho duyet diem (UC-29) [GET /api/score-approvals]
 *   52  approveScore()                  Truong ban phe duyet diem danh gia (UC-31) [POST /api/score-approvals/article/{articleId}/approve]
 *   64  returnScore()                   Truong ban yeu cau cham lai (UC-32) [POST /api/score-approvals/article/{articleId}/return]
 *   77  getPendingPublishing()          Lay danh sach bai viet cho duyet xuat ban (UC-33) [GET /api/score-approvals/publishing]
 *   88  approvePublishing()             Truong ban phe duyet xuat ban bai viet (UC-33) [POST /api/score-approvals/article/{articleId}/publish]
 */
@RestController
@RequestMapping("/api/score-approvals")
@RequiredArgsConstructor
public class ScoreApprovalController {

    private final ScoreApprovalService scoreApprovalService;

    /**
     * Lay danh sach bai viet cho duyet diem (UC-29).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScoreApprovalItemResponse>>> getPendingApprovals(
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<ScoreApprovalItemResponse> list = scoreApprovalService.getPendingApprovals(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chờ duyệt điểm thành công", list));
    }

    /**
     * Truong ban phe duyet diem danh gia (UC-31).
     */
    @PostMapping("/article/{articleId}/approve")
    public ResponseEntity<ApiResponse<ScoreApprovalResponse>> approveScore(
            @PathVariable Integer articleId,
            @AuthenticationPrincipal Jwt jwt) {
        Integer headId = Integer.valueOf(jwt.getSubject());
        ScoreApprovalResponse response = scoreApprovalService.approveScore(articleId, headId);
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt điểm đánh giá thành công", response));
    }

    /**
     * Truong ban yeu cau cham lai (UC-32).
     */
    @PostMapping("/article/{articleId}/return")
    public ResponseEntity<ApiResponse<ScoreApprovalResponse>> returnScore(
            @PathVariable Integer articleId,
            @Valid @RequestBody ScoreApprovalDecisionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer headId = Integer.valueOf(jwt.getSubject());
        ScoreApprovalResponse response = scoreApprovalService.returnScore(articleId, request, headId);
        return ResponseEntity.ok(ApiResponse.success("Yêu cầu chấm lại thành công", response));
    }

    /**
     * Lay danh sach bai viet cho duyet xuat ban (UC-33).
     */
    @GetMapping("/publishing")
    public ResponseEntity<ApiResponse<List<PublishingItemResponse>>> getPendingPublishing(
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<PublishingItemResponse> list = scoreApprovalService.getPendingPublishing(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chờ duyệt xuất bản thành công", list));
    }

    /**
     * Truong ban phe duyet xuat ban bai viet (UC-33).
     */
    @PostMapping("/article/{articleId}/publish")
    public ResponseEntity<ApiResponse<PublishingResponse>> approvePublishing(
            @PathVariable Integer articleId,
            @AuthenticationPrincipal Jwt jwt) {
        Integer headId = Integer.valueOf(jwt.getSubject());
        PublishingResponse response = scoreApprovalService.approvePublishing(articleId, headId);
        return ResponseEntity.ok(ApiResponse.success("Duyệt xuất bản bài viết thành công", response));
    }
}

