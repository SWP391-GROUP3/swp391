package com.swp391.kpi.service;

import com.swp391.kpi.dto.ArticleDetailResponse;
import com.swp391.kpi.dto.AssignmentResponse;
import com.swp391.kpi.dto.UserAccessResponse;
import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleAuthor;
import com.swp391.kpi.entity.ArticleAuthorConfirmStatus;
import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.ArticleAuthorRepository;
import com.swp391.kpi.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Xac nhan dong tac gia BF-01 (UC-12, BR-IN-06): dong y, tu choi co ly do,
 * va job het han 3 ngay (UC-12 A2). Doi trang thai bai qua ArticleService.updateArticleStatus().
 *
 * MUC LUC (so dong - ten - tac dung)
 *   47  hang so                         ten tham so, ten hanh dong, gioi han... (khong viet so ma trong code)
 *   53  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   66  acceptCoAuthorship()            UC-12 buoc 2-4: dong tac gia dong y
 *  103  rejectCoAuthorship()            UC-12 A1: dong tac gia tu choi kem ly do
 *  141  expireOverdueConfirmations()    UC-12 A2 (job chay moi gio): dong tac gia khong tra loi qua han -> EXPIRED, bai ve DRAFT, bao moi tac gia
 *  ---- Ham phu (private, chi dung trong file) ----
 *  185  findMyPendingRow()              UC-12 Preconditions: bai PENDING_CONFIRMATION, nguoi dung la dong tac gia dang PENDING, chua qua han
 *  211  findArticle()                   Tim bai theo ma, khong co thi bao 404 (NotFoundException)
 *  217  userName()                      Ho ten nguoi dung de ghi vao thong bao (qua UserReadService)
 *  227  shorten()                       notifications.message toi da 500 ky tu
 */
@Service
@RequiredArgsConstructor
public class ArticleConfirmationService {

    // Ten hanh dong ghi vao article_status_logs.action (toi da 20 ky tu)
    private static final String ACTION_CONFIRM = "CONFIRM";        // giong database/data.sql
    private static final String ACTION_CO_REJECT = "CO_REJECT";    // khac REJECT cua BTV (nhom truong 24/10, B11)
    private static final String ACTION_EXPIRE = "EXPIRE";

    private static final int MAX_MESSAGE_LENGTH = 500;   // notifications.message NVARCHAR(500)

    private final ArticleRepository articleRepository;
    private final ArticleAuthorRepository articleAuthorRepository;
    private final ArticleService articleService;
    // Du lieu luong khac -> goi service cua ho (AGENTS.md muc 2)
    private final AssignmentService assignmentService;
    private final NotificationService notificationService;
    private final UserReadService userReadService;

    /**
     * UC-12 buoc 2-4: dong tac gia dong y. Tat ca dong y -> SUBMITTED (lan nop dau)
     * hoac ASSIGNED dung BTV cu (bai nop lai, quyet dinh 12/10 - BR-IN-07).
     */
    @Transactional
    public ArticleDetailResponse acceptCoAuthorship(Integer articleId, Integer currentUserId) {
        Article article = findArticle(articleId);
        ArticleAuthor myRow = findMyPendingRow(article, currentUserId);

        myRow.setConfirmStatus(ArticleAuthorConfirmStatus.ACCEPTED);
        myRow.setRespondedAt(LocalDateTime.now());
        articleAuthorRepository.save(myRow);

        // Con ai chua dong y thi bai van cho
        for (ArticleAuthor row : articleAuthorRepository.findByArticleIdOrderBySharePercentDesc(articleId)) {
            if (!row.getUserId().equals(article.getAuthorId())
                    && row.getConfirmStatus() != ArticleAuthorConfirmStatus.ACCEPTED) {
                return articleService.getArticleDetail(articleId, currentUserId);
            }
        }

        Optional<AssignmentResponse> assignment = assignmentService.getAssignmentByArticleId(articleId);
        if (assignment.isPresent()) {
            articleService.updateArticleStatus(articleId, ArticleStatus.ASSIGNED, currentUserId, ACTION_CONFIRM, null);
            notificationService.notifyUser(assignment.get().getEditorId(), articleId, shorten(
                    "Bài \"" + article.getTitle() + "\" đã được tác giả sửa và nộp lại."));
        } else {
            articleService.updateArticleStatus(articleId, ArticleStatus.SUBMITTED, currentUserId, ACTION_CONFIRM, null);
            notificationService.notifyHeads(articleId, article.getSectionId(), shorten(
                    "Bài \"" + article.getTitle() + "\" vừa được nộp, chờ phân công biên tập viên."));
        }
        // UC-12 buoc 4: bao tac gia chinh
        notificationService.notifyUser(article.getAuthorId(), articleId, shorten(
                "Mọi đồng tác giả đã đồng ý, bài \"" + article.getTitle() + "\" đã được nộp."));
        return articleService.getArticleDetail(articleId, currentUserId);
    }

    /**
     * UC-12 A1: dong tac gia tu choi kem ly do. Bai ve DRAFT, cau tra loi cua nhung nguoi khac
     * bi xoa (ve PENDING), bao tac gia chinh kem ly do.
     */
    @Transactional
    public ArticleDetailResponse rejectCoAuthorship(Integer articleId, Integer currentUserId, String reason) {
        Article article = findArticle(articleId);
        ArticleAuthor myRow = findMyPendingRow(article, currentUserId);
        String trimmedReason = reason.trim();
        LocalDateTime now = LocalDateTime.now();

        // CHECK ck_article_authors_reject: REJECTED thi phai co reject_reason
        myRow.setConfirmStatus(ArticleAuthorConfirmStatus.REJECTED);
        myRow.setRejectReason(trimmedReason);
        myRow.setRespondedAt(now);
        articleAuthorRepository.save(myRow);

        // "the other answers are cleared": dong tac gia khac ve PENDING, bo han (lan nop sau dat han moi)
        for (ArticleAuthor row : articleAuthorRepository.findByArticleIdOrderBySharePercentDesc(articleId)) {
            if (row.getUserId().equals(article.getAuthorId()) || row.getUserId().equals(currentUserId)) {
                continue;
            }
            row.setConfirmStatus(ArticleAuthorConfirmStatus.PENDING);
            row.setConfirmDeadline(null);
            row.setRespondedAt(null);
            row.setRejectReason(null);
            articleAuthorRepository.save(row);
        }

        articleService.updateArticleStatus(articleId, ArticleStatus.DRAFT, currentUserId, ACTION_CO_REJECT, trimmedReason);
        notificationService.notifyUser(article.getAuthorId(), articleId, shorten(
                userName(currentUserId) + " từ chối làm đồng tác giả bài \"" + article.getTitle()
                        + "\". Lý do: " + trimmedReason));
        return articleService.getArticleDetail(articleId, currentUserId);
    }

    /**
     * UC-12 A2 (job chay moi gio): dong tac gia khong tra loi qua han -> EXPIRED, bai ve DRAFT,
     * bao moi tac gia. Nguoi thao tac NULL = he thong (nhom truong 24/10, A6).
     *
     * @return so bai da dua ve Nhap
     */
    @Transactional
    public int expireOverdueConfirmations() {
        LocalDateTime now = LocalDateTime.now();
        List<ArticleAuthor> overdueRows = articleAuthorRepository
                .findByConfirmStatusAndConfirmDeadlineBefore(ArticleAuthorConfirmStatus.PENDING, now);

        // Gom cac dong qua han theo bai
        Map<Integer, List<ArticleAuthor>> rowsByArticle = new LinkedHashMap<>();
        for (ArticleAuthor row : overdueRows) {
            if (!rowsByArticle.containsKey(row.getArticleId())) {
                rowsByArticle.put(row.getArticleId(), new ArrayList<>());
            }
            rowsByArticle.get(row.getArticleId()).add(row);
        }

        int expiredArticles = 0;
        for (Map.Entry<Integer, List<ArticleAuthor>> entry : rowsByArticle.entrySet()) {
            Article article = articleRepository.findById(entry.getKey()).orElse(null);
            // Chi xu ly bai dang cho xac nhan (dong PENDING o bai khac la du lieu cu, bo qua)
            if (article == null || article.getStatus() != ArticleStatus.PENDING_CONFIRMATION) {
                continue;
            }
            for (ArticleAuthor row : entry.getValue()) {
                row.setConfirmStatus(ArticleAuthorConfirmStatus.EXPIRED);
                articleAuthorRepository.save(row);
            }
            articleService.updateArticleStatus(article.getId(), ArticleStatus.DRAFT, null, ACTION_EXPIRE,
                    "Quá hạn xác nhận đồng tác giả.");

            String message = shorten("Bài \"" + article.getTitle()
                    + "\" đã về Nháp vì quá hạn mà chưa đủ đồng tác giả xác nhận.");
            for (ArticleAuthor row : articleAuthorRepository.findByArticleIdOrderBySharePercentDesc(article.getId())) {
                notificationService.notifyUser(row.getUserId(), article.getId(), message);
            }
            expiredArticles++;
        }
        return expiredArticles;
    }

    // ---------------------------------------------------------------- ham phu

    /**
     * UC-12 Preconditions: bai PENDING_CONFIRMATION, nguoi dung la dong tac gia dang PENDING,
     * chua qua han. Sai thi bao loi (E1: da tra loi / bai khong con cho).
     */
    private ArticleAuthor findMyPendingRow(Article article, Integer currentUserId) {
        if (article.getAuthorId().equals(currentUserId)) {
            throw new BusinessException("Tác giả chính không cần xác nhận bài của mình.");
        }
        if (article.getStatus() != ArticleStatus.PENDING_CONFIRMATION) {
            throw new BusinessException("Bài không còn chờ xác nhận đồng tác giả.");
        }
        ArticleAuthor myRow = null;
        for (ArticleAuthor row : articleAuthorRepository.findByArticleIdOrderBySharePercentDesc(article.getId())) {
            if (row.getUserId().equals(currentUserId)) {
                myRow = row;
            }
        }
        if (myRow == null) {
            throw new BusinessException("Bạn không phải đồng tác giả của bài này.");
        }
        if (myRow.getConfirmStatus() != ArticleAuthorConfirmStatus.PENDING) {
            throw new BusinessException("Bạn đã trả lời lời mời này rồi.");
        }
        if (myRow.getConfirmDeadline() != null && myRow.getConfirmDeadline().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Đã quá hạn xác nhận đồng tác giả.");
        }
        return myRow;
    }

    // Tim bai theo ma, khong co thi bao 404 (NotFoundException)
    private Article findArticle(Integer articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết có mã " + articleId + "."));
    }

    // Ho ten nguoi dung de ghi vao thong bao (qua UserReadService)
    private String userName(Integer userId) {
        for (UserAccessResponse user : userReadService.getUsers()) {
            if (user.id().equals(userId)) {
                return user.fullName();
            }
        }
        return "Một đồng tác giả";
    }

    // notifications.message toi da 500 ky tu
    private String shorten(String message) {
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH - 1) + "…";
    }
}
