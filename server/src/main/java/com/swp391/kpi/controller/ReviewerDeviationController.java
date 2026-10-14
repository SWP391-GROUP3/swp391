package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.ReviewerDeviationResponse;
import com.swp391.kpi.service.ReviewerDeviationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports/reviewer-deviations")
@RequiredArgsConstructor
public class ReviewerDeviationController {

    private final ReviewerDeviationService reviewerDeviationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReviewerDeviationResponse>>> getReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period,
            @RequestParam(required = false) Integer editorId,
            @RequestParam(required = false) Integer sectionId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<ReviewerDeviationResponse> data =
                reviewerDeviationService.getReport(period, editorId, sectionId, currentUserId);
        String message = data.isEmpty()
                ? "Chưa có dữ liệu độ lệch trong kỳ đã chọn."
                : "Lấy báo cáo độ lệch điểm thành công";
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }
}
