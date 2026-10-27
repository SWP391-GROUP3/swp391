package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.ArticleCoAuthorCandidateResponse;
import com.swp391.kpi.dto.ArticleCoAuthorInviteResponse;
import com.swp391.kpi.dto.ArticleCoAuthorRejectRequest;
import com.swp391.kpi.dto.ArticleDetailResponse;
import com.swp391.kpi.dto.ArticlePageResponse;
import com.swp391.kpi.dto.ArticleRevisionRequestResponse;
import com.swp391.kpi.dto.ArticleSaveRequest;
import com.swp391.kpi.dto.ArticleStatusCountResponse;
import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.service.ArticleConfirmationService;
import com.swp391.kpi.service.ArticleEditorService;
import com.swp391.kpi.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API bai viet BF-01. Nguoi thao tac lay tu token dang nhap (AGENTS.md muc 3.8):
 * subject cua JWT la id nguoi dung.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   58  field                           service duoc inject
 *   67  getMyArticles()                 UC-01: bai cua toi. Loc nhieu trang thai ngan bang dau phay, khong truyen = tat ca. [GET /api/articles?status=ASSIGNED,PENDING_APPROVAL&page=0&size=10]
 *   82  getMyStatusCounts()             Trang chu Phong vien: so bai theo tung trang thai [GET /api/articles/status-counts]
 *   94  getMyRevisionRequests()         Trang chu Phong vien: bai bi tra ve can sua kem ghi chu moi nhat [GET /api/articles/revision-requests]
 *  106  getMyCoAuthorInvites()          Trang chu Phong vien: loi moi dong tac gia dang cho minh tra loi [GET /api/articles/co-author-invites]
 *  118  getArticleDetail()              UC-02: chi tiet 1 bai [GET /api/articles/{id}]
 *  131  withdrawArticle()               UC-11: rut bai da nop ve Nhap [POST /api/articles/{id}/withdraw]
 *  144  getCoAuthorCandidates()         UC-04: danh sach phong vien chon lam dong tac gia [GET /api/articles/co-author-candidates]
 *  156  createDraft()                   UC-03 + UC-04: tao ban nhap [POST /api/articles]
 *  169  updateDraft()                   UC-03 + UC-04: sua ban nhap (DRAFT hoac REVISION_REQUESTED) [PUT /api/articles/{id}]
 *  183  submitArticle()                 UC-05: nop bai. [POST /api/articles/{id}/submit]
 *  196  resubmitArticle()               UC-10: nop lai bai bi tra ve [POST /api/articles/{id}/resubmit]
 *  209  acceptCoAuthorship()            UC-12: dong tac gia dong y [POST /api/articles/{id}/co-author/accept]
 *  222  rejectCoAuthorship()            UC-12 A1: dong tac gia tu choi, bat buoc ly do [POST /api/articles/{id}/co-author/reject]
 */
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleEditorService articleEditorService;               // Iter 2: soan, nop, nop lai
    private final ArticleConfirmationService articleConfirmationService;   // Iter 2: xac nhan dong tac gia

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

    /**
     * UC-04: danh sach phong vien chon lam dong tac gia.
     * GET /api/articles/co-author-candidates
     */
    @GetMapping("/co-author-candidates")
    public ResponseEntity<ApiResponse<List<ArticleCoAuthorCandidateResponse>>> getCoAuthorCandidates(
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<ArticleCoAuthorCandidateResponse> data = articleEditorService.getCoAuthorCandidates(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phóng viên thành công", data));
    }

    /**
     * UC-03 + UC-04: tao ban nhap.
     * POST /api/articles
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> createDraft(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ArticleSaveRequest request) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        ArticleDetailResponse data = articleEditorService.createDraft(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đã lưu bản nháp", data));
    }

    /**
     * UC-03 + UC-04: sua ban nhap (DRAFT hoac REVISION_REQUESTED).
     * PUT /api/articles/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> updateDraft(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @Valid @RequestBody ArticleSaveRequest request) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        ArticleDetailResponse data = articleEditorService.updateDraft(id, currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Đã lưu bản nháp", data));
    }

    /**
     * UC-05: nop bai.
     * POST /api/articles/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> submitArticle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        ArticleDetailResponse data = articleEditorService.submitArticle(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đã nộp bài", data));
    }

    /**
     * UC-10: nop lai bai bi tra ve.
     * POST /api/articles/{id}/resubmit
     */
    @PostMapping("/{id}/resubmit")
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> resubmitArticle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        ArticleDetailResponse data = articleEditorService.resubmitArticle(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đã nộp lại bài", data));
    }

    /**
     * UC-12: dong tac gia dong y.
     * POST /api/articles/{id}/co-author/accept
     */
    @PostMapping("/{id}/co-author/accept")
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> acceptCoAuthorship(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        ArticleDetailResponse data = articleConfirmationService.acceptCoAuthorship(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận đồng tác giả", data));
    }

    /**
     * UC-12 A1: dong tac gia tu choi, bat buoc ly do.
     * POST /api/articles/{id}/co-author/reject   body { "reason": "..." }
     */
    @PostMapping("/{id}/co-author/reject")
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> rejectCoAuthorship(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @Valid @RequestBody ArticleCoAuthorRejectRequest request) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        ArticleDetailResponse data = articleConfirmationService.rejectCoAuthorship(id, currentUserId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối làm đồng tác giả", data));
    }
}
