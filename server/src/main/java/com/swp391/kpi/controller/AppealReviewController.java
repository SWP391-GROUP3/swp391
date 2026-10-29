package com.swp391.kpi.controller;

import com.swp391.kpi.dto.*;
import com.swp391.kpi.service.AppealReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * API cham lai mu va chot khieu nai (UC-37...39): diem hieu luc cua bai, hang cho va phieu cham mu cua
 * BTV, luu / nop phieu, so sanh diem, chot ket qua.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   29  field                           service duoc inject
 *   32  effectiveScore()                effective score [GET /api/articles/{id}/effective-score]
 *   37  queue()                         queue [GET /api/appeal-reviews]
 *   41  card()                          card [GET /api/appeal-reviews/{id}]
 *   45  draft()                         draft [PUT /api/appeal-reviews/{id}]
 *   50  submit()                        nop [POST /api/appeal-reviews/{id}/submit]
 *   55  comparison()                    comparison [GET /api/appeals/{id}/comparison]
 *   59  finalizeAppeal()                chot appeal [POST /api/appeals/{id}/finalize]
 */
@RestController @RequiredArgsConstructor
public class AppealReviewController {
    private final AppealReviewService service;

    @GetMapping("/api/articles/{id}/effective-score")
    public ResponseEntity<ApiResponse<AppealEffectiveScoreResponse>> effectiveScore(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Lấy điểm hiệu lực thành công", service.getEffectiveScore(id, Integer.valueOf(jwt.getSubject()))));
    }

    @GetMapping("/api/appeal-reviews")
    public ResponseEntity<ApiResponse<List<AppealReviewTaskResponse>>> queue(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách chấm mù thành công", service.getQueue(Integer.valueOf(jwt.getSubject()))));
    }
    @GetMapping("/api/appeal-reviews/{id}")
    public ResponseEntity<ApiResponse<AppealBlindScorecardResponse>> card(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Lấy phiếu chấm mù thành công", service.getCard(id, Integer.valueOf(jwt.getSubject()))));
    }
    @PutMapping("/api/appeal-reviews/{id}")
    public ResponseEntity<ApiResponse<AppealBlindScorecardResponse>> draft(@PathVariable Integer id,
            @Valid @RequestBody AppealScorecardRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Đã lưu nháp", service.save(id, Integer.valueOf(jwt.getSubject()), request, false)));
    }
    @PostMapping("/api/appeal-reviews/{id}/submit")
    public ResponseEntity<ApiResponse<AppealBlindScorecardResponse>> submit(@PathVariable Integer id,
            @Valid @RequestBody AppealScorecardRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Đã nộp và khóa phiếu chấm", service.save(id, Integer.valueOf(jwt.getSubject()), request, true)));
    }
    @GetMapping("/api/appeals/{id}/comparison")
    public ResponseEntity<ApiResponse<AppealComparisonResponse>> comparison(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Lấy so sánh điểm thành công", service.getComparison(id, Integer.valueOf(jwt.getSubject()))));
    }
    @PostMapping("/api/appeals/{id}/finalize")
    public ResponseEntity<ApiResponse<AppealComparisonResponse>> finalizeAppeal(@PathVariable Integer id,
            @Valid @RequestBody AppealFinalizeRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận điểm hiệu lực", service.finalizeAppeal(id, Integer.valueOf(jwt.getSubject()), request)));
    }
}
