package com.swp391.kpi.appeal.controller;

import com.swp391.kpi.appeal.dto.ReviewerDirectoryResponse;
import com.swp391.kpi.appeal.dto.ReviewerProfileResponse;
import com.swp391.kpi.appeal.dto.ReviewerScoringHistoryResponse;
import com.swp391.kpi.appeal.service.ReviewerProfileService;
import com.swp391.kpi.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

/**
 * API ho so bien tap vien (UC-42): danh sach BTV, ho so chat luong, lich su cham.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   31  field                           service duoc inject
 *   34  getEditors()                    lay editors [GET /api/reviewers]
 *   40  getProfile()                    lay profile [GET /api/reviewers/{editorId}]
 *   48  getHistory()                    lay history [GET /api/reviewers/{editorId}/scoring-history]
 */
@RestController
@RequestMapping("/api/reviewers")
@RequiredArgsConstructor
public class ReviewerProfileController {
    private final ReviewerProfileService reviewerProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewerDirectoryResponse>>> getEditors(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách biên tập viên thành công",
                reviewerProfileService.getEditors(Integer.valueOf(jwt.getSubject()))));
    }

    @GetMapping("/{editorId}")
    public ResponseEntity<ApiResponse<ReviewerProfileResponse>> getProfile(@PathVariable Integer editorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ biên tập viên thành công",
                reviewerProfileService.getProfile(editorId, period, Integer.valueOf(jwt.getSubject()))));
    }

    @GetMapping("/{editorId}/scoring-history")
    public ResponseEntity<ApiResponse<Page<ReviewerScoringHistoryResponse>>> getHistory(@PathVariable Integer editorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử chấm điểm thành công",
                reviewerProfileService.getHistory(editorId, period, page, size, Integer.valueOf(jwt.getSubject()))));
    }
}
