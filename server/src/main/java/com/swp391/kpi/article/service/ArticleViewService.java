package com.swp391.kpi.article.service;

import com.swp391.kpi.admin.dto.UserAccessResponse;
import com.swp391.kpi.admin.entity.RoleCode;
import com.swp391.kpi.admin.service.UserReadService;
import com.swp391.kpi.article.dto.ArticleDetailResponse;
import com.swp391.kpi.article.entity.Article;
import com.swp391.kpi.article.entity.ArticleStatus;
import com.swp391.kpi.article.repository.ArticleRepository;
import com.swp391.kpi.assessment.dto.AssignmentResponse;
import com.swp391.kpi.assessment.service.AssignmentService;
import com.swp391.kpi.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;

/**
 * UC-02 xem chi tiet bai: kiem ai duoc xem roi moi dung du lieu qua ArticleService.buildArticleDetail().
 * Tach rieng khoi ArticleService vi can AssignmentService (BTV duoc giao), ma AssignmentService
 * da goi ArticleService -> dat trong ArticleService se thanh vong phu thuoc (quyet dinh 03/11).
 *
 * CODE LIEN QUAN (duong dan tinh tu server/src/main/java/com/swp391/kpi/):
 *   Duoc goi tu: article/controller/ArticleController.java (getArticleDetail);
 *     ArticleAttachmentService, DuplicationCheckService (checkCanView truoc khi tra anh / ket qua kiem trung)
 *   Goi sang: article/service/ArticleService.java (buildArticleDetail, isAuthorOrCoAuthor),
 *     assessment/service/AssignmentService.java (BTV duoc giao), admin/service/UserReadService.java (vai tro, ban)
 *
 * MUC LUC (so dong - ten - tac dung)
 *   45  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   56  getArticleDetail()              UC-02: chi tiet 1 bai cho tac gia, BTV duoc giao, Truong ban cung ban; nguoi khac 403
 *   66  checkCanView()                  Bai khong ton tai -> 404, nguoi dung khong duoc xem -> 403; duoc xem thi tra ve bai
 *  ---- Ham phu (private, chi dung trong file) ----
 *   76  canView()                       Nguoi dung co duoc xem bai khong (BR-IN-01, B18, B19)
 */
@Service
@RequiredArgsConstructor
public class ArticleViewService {

    private final ArticleRepository articleRepository;
    private final ArticleService articleService;
    // BTV duoc giao la du lieu luong assessment -> goi service cua ho (AGENTS.md muc 2)
    private final AssignmentService assignmentService;
    // Vai tro va ban cua nguoi dung la du lieu luong admin
    private final UserReadService userReadService;

    /**
     * UC-02: chi tiet 1 bai cho tac gia, BTV duoc giao, Truong ban cung ban; nguoi khac 403.
     */
    @Transactional(readOnly = true)
    public ArticleDetailResponse getArticleDetail(Integer articleId, Integer currentUserId) {
        checkCanView(articleId, currentUserId);
        return articleService.buildArticleDetail(articleId, currentUserId);
    }

    /**
     * Bai khong ton tai -> 404, nguoi dung khong duoc xem -> 403; duoc xem thi tra ve bai.
     * Dung chung cho chi tiet bai va anh/video cua bai (B8: cung nhom nguoi xem).
     */
    @Transactional(readOnly = true)
    public Article checkCanView(Integer articleId, Integer currentUserId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết có mã " + articleId + "."));
        if (!canView(article, currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem bài viết này.");
        }
        return article;
    }

    // Nguoi dung co duoc xem bai khong (BR-IN-01, B18, B19)
    private boolean canView(Article article, Integer userId) {
        // Tac gia chinh hoac dong tac gia (BR-IN-01: dong tac gia chi xem)
        if (articleService.isAuthorOrCoAuthor(article.getId(), userId)) {
            return true;
        }

        // B18: BTV la editor_id cua dong assignments cua bai. Giao lai BTV khac thi BTV cu mat quyen (D7)
        Optional<AssignmentResponse> assignment = assignmentService.getAssignmentByArticleId(article.getId());
        if (assignment.isPresent() && assignment.get().getEditorId().equals(userId)) {
            return true;
        }

        // B19: Truong ban xem bai cung ban, bai da nop (khac DRAFT)
        if (article.getStatus() == ArticleStatus.DRAFT) {
            return false;
        }
        for (UserAccessResponse user : userReadService.getUsers()) {
            if (user.id().equals(userId)) {
                return RoleCode.HEAD_OF_DEPARTMENT.name().equals(user.role())
                        && Objects.equals(article.getSectionId(), user.sectionId());
            }
        }
        return false;
    }
}
