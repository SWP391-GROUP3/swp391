package com.swp391.kpi.controller;

import com.swp391.kpi.dto.*;
import com.swp391.kpi.service.ReviewerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

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
