package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.ArticleCoAuthorInviteResponse;
import com.swp391.kpi.dto.ArticleDetailResponse;
import com.swp391.kpi.dto.ArticlePageResponse;
import com.swp391.kpi.dto.ArticleRevisionRequestResponse;
import com.swp391.kpi.dto.ArticleStatusCountResponse;
import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API bai viet BF-01. Nguoi thao tac lay tu token dang nhap (AGENTS.md muc 3.8):
 * subject cua JWT la id nguoi dung.
 */
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * UC-01: bai cua toi. Loc nhieu trang thai ngan bang dau phay, khong truyen = tat ca.
     * GET /api/articles?status=ASSIGNED,PENDING_APPROVAL&page=0&size=10
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ArticlePageResponse>> getMyArticles(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) List<ArticleStatus> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());   // id nguoi dang nhap, lay tu token
        ArticlePageResponse data = articleService.getMyArticles(currentUserId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài viết thành công", data));
    }

    /**
     * Trang chu Phong vien: so bai theo tung trang thai.
     * GET /api/articles/status-counts
     */
    @GetMapping("/status-counts")
    public ResponseEntity<ApiResponse<List<ArticleStatusCountResponse>>> getMyStatusCounts(
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<ArticleStatusCountResponse> data = articleService.getMyStatusCounts(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy số bài theo trạng thái thành công", data));
    }

    /**
     * Trang chu Phong vien: bai bi tra ve can sua kem ghi chu moi nhat.
     * GET /api/articles/revision-requests
     */
    @GetMapping("/revision-requests")
    public ResponseEntity<ApiResponse<List<ArticleRevisionRequestResponse>>> getMyRevisionRequests(
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<ArticleRevisionRequestResponse> data = articleService.getMyRevisionRequests(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài bị trả về thành công", data));
    }

    /**
     * Trang chu Phong vien: loi moi dong tac gia dang cho minh tra loi.
     * GET /api/articles/co-author-invites
     */
    @GetMapping("/co-author-invites")
    public ResponseEntity<ApiResponse<List<ArticleCoAuthorInviteResponse>>> getMyCoAuthorInvites(
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<ArticleCoAuthorInviteResponse> data = articleService.getMyCoAuthorInvites(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy lời mời đồng tác giả thành công", data));
    }

    /**
     * UC-02: chi tiet 1 bai.
     * GET /api/articles/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> getArticleDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        ArticleDetailResponse data = articleService.getArticleDetail(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết bài viết thành công", data));
    }

    /**
     * UC-11: rut bai da nop ve Nhap.
     * POST /api/articles/{id}/withdraw
     */
    @PostMapping("/{id}/withdraw")
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> withdrawArticle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        ArticleDetailResponse data = articleService.withdrawArticle(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đã rút bài về trạng thái Nháp", data));
    }
}
