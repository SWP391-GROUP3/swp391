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
/**
 * API phieu cham cua BTV (UC-22...28): mo phieu cham (chup bo tieu chi dang ACTIVE), doi the loai, luu
 * diem nhap, nop quyet dinh, xem lan cham moi nhat.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   32  field                           service duoc inject
 *   36  openScoringAttempt()            Mo phieu cham cho bai viet va snapshot bo tieu chi ACTI… [POST /api/scoring-attempts/article/{articleId}/open]
 *   46  changeCategory()                BTV doi the loai bai viet truoc khi cham (UC… [POST /api/scoring-attempts/article/{articleId}/change-category]
 *   57  saveScores()                    BTV luu diem tung tieu chi va tinh tong diem theo tron… [PUT /api/scoring-attempts/article/{articleId}/scores]
 *   68  submitDecision()                BTV nop phieu cham voi quyet dinh ACCEPT, REVISION ho… [POST /api/scoring-attempts/article/{articleId}/submit]
 *   79  getLatestAttempt()              Lay lan cham gan nhat cua mot bai viet (UC-30, UC-34) [GET /api/scoring-attempts/article/{articleId}/latest]
 */
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

    // BTV luu diem tung tieu chi va tinh tong diem theo trong so (UC-24, UC-25, BR-SC-06, BR-SC-07)
    @PutMapping("/article/{articleId}/scores")
    public ResponseEntity<ApiResponse<ScoringAttemptResponse>> saveScores(
            @PathVariable Integer articleId,
            @Valid @RequestBody com.swp391.kpi.dto.ScoringSaveRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer editorId = Integer.valueOf(jwt.getSubject());
        ScoringAttemptResponse response = scoringAttemptService.saveDraftScores(articleId, request, editorId);
        return ResponseEntity.ok(ApiResponse.success("Lưu điểm tiêu chí thành công", response));
    }

    // BTV nop phieu cham voi quyet dinh ACCEPT, REVISION hoac REJECT (UC-26, UC-27, UC-28)
    @PostMapping("/article/{articleId}/submit")
    public ResponseEntity<ApiResponse<ScoringAttemptResponse>> submitDecision(
            @PathVariable Integer articleId,
            @Valid @RequestBody com.swp391.kpi.dto.ScoringSubmitRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer editorId = Integer.valueOf(jwt.getSubject());
        ScoringAttemptResponse response = scoringAttemptService.submitDecision(articleId, request, editorId);
        return ResponseEntity.ok(ApiResponse.success("Nộp phiếu chấm điểm thành công", response));
    }

    // Lay lan cham gan nhat cua mot bai viet (UC-30, UC-34)
    @GetMapping("/article/{articleId}/latest")
    public ResponseEntity<ApiResponse<ScoringAttemptResponse>> getLatestAttempt(
            @PathVariable Integer articleId,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        ScoringAttemptResponse response = scoringAttemptService.getLatestAttemptForUser(articleId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phiếu chấm thành công", response));
    }
}
