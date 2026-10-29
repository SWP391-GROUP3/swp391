package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.AppealCreateRequest;
import com.swp391.kpi.dto.AppealEligibilityResponse;
import com.swp391.kpi.dto.AppealResponse;
import com.swp391.kpi.service.AppealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API khieu nai cua Phong vien (UC-35): bai co the khieu nai, kiem tra dieu kien, gui khieu nai, xem
 * khieu nai cua toi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   39  field                           service duoc inject
 *   42  getEligibleArticles()           lay eligible articles [GET /api/appeals/eligible-articles]
 *   50  submitAppeal()                  nop appeal [POST /api/appeals]
 *   60  getMyAppeals()                  lay my appeals [GET /api/appeals/mine]
 *   68  getEligibility()                lay eligibility [GET /api/articles/{articleId}/appeal-eligibility]
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppealController {

    private final AppealService appealService;

    @GetMapping("/appeals/eligible-articles")
    public ResponseEntity<ApiResponse<List<AppealEligibilityResponse>>> getEligibleArticles(
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Lấy bài có thể khiếu nại thành công",
                appealService.getEligibleArticles(currentUserId)));
    }

    @PostMapping("/appeals")
    public ResponseEntity<ApiResponse<AppealResponse>> submitAppeal(
            @Valid @RequestBody AppealCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        AppealResponse data = appealService.submitAppeal(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Gửi khiếu nại thành công", data));
    }

    @GetMapping("/appeals/mine")
    public ResponseEntity<ApiResponse<List<AppealResponse>>> getMyAppeals(
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<AppealResponse> data = appealService.getMyAppeals(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khiếu nại thành công", data));
    }

    @GetMapping("/articles/{articleId}/appeal-eligibility")
    public ResponseEntity<ApiResponse<AppealEligibilityResponse>> getEligibility(
            @PathVariable Integer articleId,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        AppealEligibilityResponse data = appealService.getEligibility(articleId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Bài viết đủ điều kiện khiếu nại", data));
    }
}
