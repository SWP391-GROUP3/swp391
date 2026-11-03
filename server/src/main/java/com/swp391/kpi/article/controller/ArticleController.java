package com.swp391.kpi.article.controller;

import com.swp391.kpi.article.dto.ArticleAttachmentFileResponse;
import com.swp391.kpi.article.dto.ArticleAttachmentResponse;
import com.swp391.kpi.article.dto.ArticleAttachmentUploadRequest;
import com.swp391.kpi.article.dto.ArticleCoAuthorCandidateResponse;
import com.swp391.kpi.article.dto.ArticleCoAuthorInviteResponse;
import com.swp391.kpi.article.dto.ArticleCoAuthorRejectRequest;
import com.swp391.kpi.article.dto.ArticleDetailResponse;
import com.swp391.kpi.article.dto.ArticlePageResponse;
import com.swp391.kpi.article.dto.ArticleRevisionRequestResponse;
import com.swp391.kpi.article.dto.ArticleRoyaltyEstimateRequest;
import com.swp391.kpi.article.dto.ArticleSaveRequest;
import com.swp391.kpi.article.dto.ArticleStatusCountResponse;
import com.swp391.kpi.article.dto.ArticleSubmitRequest;
import com.swp391.kpi.article.dto.DuplicationCheckResponse;
import com.swp391.kpi.article.entity.ArticleStatus;
import com.swp391.kpi.article.service.ArticleAttachmentService;
import com.swp391.kpi.article.service.ArticleConfirmationService;
import com.swp391.kpi.article.service.ArticleEditorService;
import com.swp391.kpi.article.service.ArticleService;
import com.swp391.kpi.article.service.ArticleViewService;
import com.swp391.kpi.article.service.DuplicationCheckService;
import com.swp391.kpi.common.dto.ApiResponse;
import com.swp391.kpi.royalty.dto.RoyaltyEstimateResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
 *   78  field                           service duoc inject
 *   90  getMyArticles()                 UC-01: bai cua toi. Loc nhieu trang thai ngan bang dau phay, khong truyen = tat ca. [GET /api/articles]
 *  105  getMyStatusCounts()             Trang chu Phong vien: so bai theo tung trang thai [GET /api/articles/status-counts]
 *  117  getMyRevisionRequests()         Trang chu Phong vien: bai bi tra ve can sua kem ghi chu moi nhat [GET /api/articles/revision-requests]
 *  129  getMyCoAuthorInvites()          Trang chu Phong vien: loi moi dong tac gia dang cho minh tra loi [GET /api/articles/co-author-invites]
 *  141  getArticleDetail()              UC-02: chi tiet 1 bai [GET /api/articles/{id}]
 *  154  withdrawArticle()               UC-11: rut bai da nop ve Nhap [POST /api/articles/{id}/withdraw]
 *  167  getCoAuthorCandidates()         UC-04: danh sach phong vien chon lam dong tac gia [GET /api/articles/co-author-candidates]
 *  179  createDraft()                   UC-03 + UC-04: tao ban nhap [POST /api/articles]
 *  192  updateDraft()                   UC-03 + UC-04: sua ban nhap (DRAFT hoac REVISION_REQUESTED) [PUT /api/articles/{id}]
 *  206  submitArticle()                 UC-05: nop bai. Body khong bat buoc: { "explanation" } khi noi dung trung ca… [POST /api/articles/{id}/submit]
 *  221  resubmitArticle()               UC-10: nop lai bai bi tra ve [POST /api/articles/{id}/resubmit]
 *  236  previewDuplicationCheck()       UC-05 buoc 2 (B5): kiem trung noi dung truoc khi xac nhan nop, ch… [POST /api/articles/{id}/duplication-check]
 *  249  getDuplicationCheck()           UC-06 / UC-08: ket qua kiem trung luc nop gan nhat (kem giai trinh) [GET /api/articles/{id}/duplication-check]
 *  262  estimateRoyalty()               UC-09: uoc tinh nhuan but cua bai theo the loai, tinh chat va su kien d… [POST /api/articles/royalty-estimate]
 *  275  acceptCoAuthorship()            UC-12: dong tac gia dong y [POST /api/articles/{id}/co-author/accept]
 *  288  rejectCoAuthorship()            UC-12 A1: dong tac gia tu choi, bat buoc ly do [POST /api/articles/{id}/co-author/reject]
 *  302  getAttachments()                UC-03 / UC-07: anh/video cua bai kem ket qua kiem trung (nguoi xem nhu c… [GET /api/articles/{id}/attachments]
 *  315  addAttachment()                 UC-03 / UC-04 buoc 4: them 1 anh (file) hoac 1 video (link) [POST /api/articles/{id}/attachments]
 *  329  deleteAttachment()              Xoa 1 anh/video khi bai con sua duoc (chi tac gia chin… [DELETE /api/articles/{id}/attachments/{attachmentId}]
 *  343  getImageFile()                  Q7: file anh de trinh duyet hien (tra thang byte anh… [GET /api/articles/{id}/attachments/{attachmentId}/file]
 */
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleEditorService articleEditorService;               // Iter 2: soan, nop, nop lai
    private final ArticleConfirmationService articleConfirmationService;   // Iter 2: xac nhan dong tac gia
    private final ArticleViewService articleViewService;                   // Iter 3: quyen xem chi tiet bai (UC-02)
    private final ArticleAttachmentService articleAttachmentService;       // Iter 3: anh/video cua bai (UC-03, UC-07)
    private final DuplicationCheckService duplicationCheckService;         // Iter 3: kiem trung noi dung (UC-06, UC-08)

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
        ArticleDetailResponse data = articleViewService.getArticleDetail(id, currentUserId);
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
     * UC-05: nop bai. Body khong bat buoc: { "explanation" } khi noi dung trung can giai trinh (UC-08).
     * POST /api/articles/{id}/submit
     */
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> submitArticle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) ArticleSubmitRequest request) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        String explanation = request == null ? null : request.getExplanation();
        ArticleDetailResponse data = articleEditorService.submitArticle(id, currentUserId, explanation);
        return ResponseEntity.ok(ApiResponse.success("Đã nộp bài", data));
    }

    /**
     * UC-10: nop lai bai bi tra ve. Body khong bat buoc giong submit.
     * POST /api/articles/{id}/resubmit
     */
    @PostMapping("/{id}/resubmit")
    public ResponseEntity<ApiResponse<ArticleDetailResponse>> resubmitArticle(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) ArticleSubmitRequest request) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        String explanation = request == null ? null : request.getExplanation();
        ArticleDetailResponse data = articleEditorService.resubmitArticle(id, currentUserId, explanation);
        return ResponseEntity.ok(ApiResponse.success("Đã nộp lại bài", data));
    }

    /**
     * UC-05 buoc 2 (B5): kiem trung noi dung truoc khi xac nhan nop, chi tinh, khong luu.
     * POST /api/articles/{id}/duplication-check
     */
    @PostMapping("/{id}/duplication-check")
    public ResponseEntity<ApiResponse<DuplicationCheckResponse>> previewDuplicationCheck(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        DuplicationCheckResponse data = duplicationCheckService.previewCheck(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đã kiểm tra trùng nội dung", data));
    }

    /**
     * UC-06 / UC-08: ket qua kiem trung luc nop gan nhat (kem giai trinh). Chua nop lan nao -> data null.
     * GET /api/articles/{id}/duplication-check
     */
    @GetMapping("/{id}/duplication-check")
    public ResponseEntity<ApiResponse<DuplicationCheckResponse>> getDuplicationCheck(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        DuplicationCheckResponse data = duplicationCheckService.getLatestCheck(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy kết quả kiểm trùng thành công", data));
    }

    /**
     * UC-09: uoc tinh nhuan but cua bai theo the loai, tinh chat va su kien dang chon (khong luu gi).
     * POST /api/articles/royalty-estimate   body { "declaredCategoryId", "declaredNature", "eventId", "articleId" }
     */
    @PostMapping("/royalty-estimate")
    public ResponseEntity<ApiResponse<RoyaltyEstimateResponse>> estimateRoyalty(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ArticleRoyaltyEstimateRequest request) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        RoyaltyEstimateResponse data = articleEditorService.estimateRoyalty(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Ước tính nhuận bút thành công", data));
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

    /**
     * UC-03 / UC-07: anh/video cua bai kem ket qua kiem trung (nguoi xem nhu chi tiet bai, B8).
     * GET /api/articles/{id}/attachments
     */
    @GetMapping("/{id}/attachments")
    public ResponseEntity<ApiResponse<List<ArticleAttachmentResponse>>> getAttachments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<ArticleAttachmentResponse> data = articleAttachmentService.getAttachments(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ảnh/video thành công", data));
    }

    /**
     * UC-03 / UC-04 buoc 4: them 1 anh (file) hoac 1 video (link). Gui multipart/form-data.
     * POST /api/articles/{id}/attachments   form: file | videoUrl, mediaKind, authorId, externalSource, caption
     */
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ArticleAttachmentResponse>> addAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @Valid @ModelAttribute ArticleAttachmentUploadRequest request) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        ArticleAttachmentResponse data = articleAttachmentService.addAttachment(id, currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Đã thêm ảnh/video", data));
    }

    /**
     * Xoa 1 anh/video khi bai con sua duoc (chi tac gia chinh).
     * DELETE /api/articles/{id}/attachments/{attachmentId}
     */
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @PathVariable Integer attachmentId) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        articleAttachmentService.deleteAttachment(id, attachmentId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa ảnh/video", null));
    }

    /**
     * Q7: file anh de trinh duyet hien (tra thang byte anh, khong boc ApiResponse).
     * GET /api/articles/{id}/attachments/{attachmentId}/file
     */
    @GetMapping("/{id}/attachments/{attachmentId}/file")
    public ResponseEntity<byte[]> getImageFile(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer id,
            @PathVariable Integer attachmentId) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        ArticleAttachmentFileResponse file = articleAttachmentService.getImageFile(id, attachmentId, currentUserId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .body(file.getContent());
    }
}
