package com.swp391.kpi.service;

import com.swp391.kpi.dto.ArticleAuthorResponse;
import com.swp391.kpi.dto.ArticleCoAuthorInviteResponse;
import com.swp391.kpi.dto.ArticleDetailResponse;
import com.swp391.kpi.dto.ArticlePageResponse;
import com.swp391.kpi.dto.ArticleRevisionRequestResponse;
import com.swp391.kpi.dto.ArticleStatusCountResponse;
import com.swp391.kpi.dto.ArticleStatusLogResponse;
import com.swp391.kpi.dto.ArticleSummaryResponse;
import com.swp391.kpi.dto.UserAccessResponse;
import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleAuthor;
import com.swp391.kpi.entity.ArticleAuthorConfirmStatus;
import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.entity.ArticleStatusLog;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.ArticleAuthorRepository;
import com.swp391.kpi.repository.ArticleRepository;
import com.swp391.kpi.repository.ArticleStatusLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nghiep vu bai viet BF-01. Moi lan doi articles.status deu di qua changeStatus()
 * de ghi kem 1 dong article_status_logs (AGENTS.md muc 2).
 *
 * MUC LUC (so dong)
 *   60  hang so                      - So dong toi da 1 trang, tranh client xin qua nhieu 1 lan
 *   65  field                        - repository / service duoc inject
 *   76  getMyArticles()              - UC-01: bai ma nguoi dung la tac gia chinh hoac dong tac gia, sua gan nh…
 *  120  getArticleDetail()           - UC-02: xem noi dung, khai bao, tac gia va lich su trang thai cua 1 bai.
 *  135  withdrawArticle()            - UC-11: tac gia chinh rut bai da nop khi chua phan cong BTV, bai ve DRAF…
 *  158  getMyStatusCounts()          - Trang chu Phong vien: so bai cua nguoi dung o tung trang thai.
 *  179  getMyRevisionRequests()      - Trang chu Phong vien: bai cua nguoi dung (tac gia chinh) bi tra ve can …
 *  199  getMyCoAuthorInvites()       - Trang chu Phong vien: loi moi dong tac gia dang cho nguoi dung tra loi …
 *  221  findArticle()                - Tim bai theo ma, khong co thi bao 404 (NotFoundException)
 *  227  isAuthor()                   - Tac gia chinh (articles.author_id) hoac co dong trong article_authors
 *  238  changeStatus()               - Doi trang thai bai va ghi 1 dong lich su trong cung transaction (AGENTS…
 *  257  toDetailResponse()           - Gom bai + tac gia + lich su (kem ho ten) thanh DTO chi tiet bai (UC-02)
 *  279  loadUserNames()              - Map userId -> ho ten, de doi ma nguoi dung thanh ten khi tra ve giao di…
 */
@Service
@RequiredArgsConstructor
public class ArticleService {

    // So dong toi da 1 trang, tranh client xin qua nhieu 1 lan
    private static final int MAX_PAGE_SIZE = 50;

    // Ten hanh dong ghi vao article_status_logs.action khi rut bai
    private static final String ACTION_WITHDRAW = "WITHDRAW";

    private final ArticleRepository articleRepository;
    private final ArticleAuthorRepository articleAuthorRepository;
    private final ArticleStatusLogRepository articleStatusLogRepository;
    // Ho ten nguoi dung la du lieu luong admin -> goi service cua ho, khong query bang users (AGENTS.md muc 2)
    private final UserReadService userReadService;

    /**
     * UC-01: bai ma nguoi dung la tac gia chinh hoac dong tac gia, sua gan nhat len dau.
     * statuses rong hoac null = lay moi trang thai.
     */
    @Transactional(readOnly = true)
    public ArticlePageResponse getMyArticles(Integer currentUserId, List<ArticleStatus> statuses, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException("Trang phải từ 0 và số dòng mỗi trang phải từ 1 đến " + MAX_PAGE_SIZE + ".");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<Article> articlePage;
        if (statuses == null || statuses.isEmpty()) {
            articlePage = articleRepository.findMyArticles(currentUserId, pageable);
        } else {
            articlePage = articleRepository.findMyArticlesByStatuses(currentUserId, statuses, pageable);
        }

        // Lay dong article_authors cua nguoi dung trong cac bai o trang nay bang 1 cau query,
        // roi dua vao Map articleId -> dong do de tra cuu vai tro va ty le
        List<Integer> articleIds = new ArrayList<>();
        for (Article article : articlePage.getContent()) {
            articleIds.add(article.getId());
        }
        Map<Integer, ArticleAuthor> myAuthorRows = new HashMap<>();
        if (!articleIds.isEmpty()) {
            for (ArticleAuthor row : articleAuthorRepository.findByUserIdAndArticleIdIn(currentUserId, articleIds)) {
                myAuthorRows.put(row.getArticleId(), row);
            }
        }

        List<ArticleSummaryResponse> items = new ArrayList<>();
        for (Article article : articlePage.getContent()) {
            items.add(ArticleSummaryResponse.from(article, myAuthorRows.get(article.getId()), currentUserId));
        }

        ArticlePageResponse result = new ArticlePageResponse();
        result.setItems(items);
        result.setPage(articlePage.getNumber());
        result.setSize(articlePage.getSize());
        result.setTotalElements(articlePage.getTotalElements());
        result.setTotalPages(articlePage.getTotalPages());
        return result;
    }

    /**
     * UC-02: xem noi dung, khai bao, tac gia va lich su trang thai cua 1 bai.
     */
    @Transactional(readOnly = true)
    public ArticleDetailResponse getArticleDetail(Integer articleId, Integer currentUserId) {
        Article article = findArticle(articleId);

        // Iter 1 chi tac gia chinh va dong tac gia duoc xem (BR-IN-01: dong tac gia chi xem).
        // TODO(BF-02): cho them BTV duoc giao va Truong ban khi co AssignmentService, User
        if (!isAuthor(article, currentUserId)) {
            throw new BusinessException("Bạn không phải tác giả của bài viết này nên không được xem.");
        }
        return toDetailResponse(article, currentUserId);
    }

    /**
     * UC-11: tac gia chinh rut bai da nop khi chua phan cong BTV, bai ve DRAFT (BR-IN-08).
     */
    @Transactional
    public ArticleDetailResponse withdrawArticle(Integer articleId, Integer currentUserId) {
        Article article = findArticle(articleId);

        // BR-IN-08: chi tac gia chinh duoc rut bai
        if (!article.getAuthorId().equals(currentUserId)) {
            throw new BusinessException("Chỉ tác giả chính mới được rút bài.");
        }
        // BR-IN-08: chi rut duoc bai SUBMITTED. Da phan cong BTV thi bai da sang ASSIGNED nen khong rut duoc
        if (article.getStatus() != ArticleStatus.SUBMITTED) {
            throw new BusinessException("Chỉ rút được bài đang ở trạng thái Đã nộp và chưa được phân công biên tập viên.");
        }

        // Theo comment bang articles: rut bai thi ghi withdrawn_at
        article.setWithdrawnAt(LocalDateTime.now());
        changeStatus(article, ArticleStatus.DRAFT, currentUserId, ACTION_WITHDRAW, null);
        return toDetailResponse(article, currentUserId);
    }

    /**
     * Trang chu Phong vien: so bai cua nguoi dung o tung trang thai.
     * Du 9 trang thai, trang thai khong co bai thi count = 0.
     */
    @Transactional(readOnly = true)
    public List<ArticleStatusCountResponse> getMyStatusCounts(Integer currentUserId) {
        List<ArticleStatus> myStatuses = articleRepository.findMyArticleStatuses(currentUserId);

        List<ArticleStatusCountResponse> result = new ArrayList<>();
        for (ArticleStatus status : ArticleStatus.values()) {
            long count = 0;
            for (ArticleStatus myStatus : myStatuses) {
                if (myStatus == status) {
                    count++;
                }
            }
            result.add(new ArticleStatusCountResponse(status.name(), count));
        }
        return result;
    }

    /**
     * Trang chu Phong vien: bai cua nguoi dung (tac gia chinh) bi tra ve can sua,
     * kem ghi chu cua lan tra ve gan nhat trong article_status_logs.
     */
    @Transactional(readOnly = true)
    public List<ArticleRevisionRequestResponse> getMyRevisionRequests(Integer currentUserId) {
        List<Article> articles = articleRepository.findByAuthorIdAndStatusOrderByUpdatedAtDesc(
                currentUserId, ArticleStatus.REVISION_REQUESTED);

        List<ArticleRevisionRequestResponse> result = new ArrayList<>();
        for (Article article : articles) {
            ArticleStatusLog returnLog = articleStatusLogRepository
                    .findFirstByArticleIdAndToStatusOrderByCreatedAtDescIdDesc(article.getId(),
                            ArticleStatus.REVISION_REQUESTED)
                    .orElse(null);
            result.add(ArticleRevisionRequestResponse.from(article, returnLog));
        }
        return result;
    }

    /**
     * Trang chu Phong vien: loi moi dong tac gia dang cho nguoi dung tra loi (UC-12),
     * sap het han xep truoc.
     */
    @Transactional(readOnly = true)
    public List<ArticleCoAuthorInviteResponse> getMyCoAuthorInvites(Integer currentUserId) {
        List<ArticleAuthor> pendingRows = articleAuthorRepository
                .findByUserIdAndConfirmStatusOrderByConfirmDeadlineAsc(currentUserId, ArticleAuthorConfirmStatus.PENDING);

        Map<Integer, String> userNames = loadUserNames();
        List<ArticleCoAuthorInviteResponse> result = new ArrayList<>();
        for (ArticleAuthor row : pendingRows) {
            Article article = articleRepository.findById(row.getArticleId()).orElse(null);
            // Chi tinh bai dang cho xac nhan va nguoi dung la dong tac gia (khong phai tac gia chinh)
            if (article == null
                    || article.getStatus() != ArticleStatus.PENDING_CONFIRMATION
                    || article.getAuthorId().equals(currentUserId)) {
                continue;
            }
            result.add(ArticleCoAuthorInviteResponse.from(article, row, userNames.get(article.getAuthorId())));
        }
        return result;
    }

    // ---------------------------------------------------------------- ham phu

    // Tim bai theo ma, khong co thi bao 404 (NotFoundException)
    private Article findArticle(Integer articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết có mã " + articleId + "."));
    }

    // Tac gia chinh (articles.author_id) hoac co dong trong article_authors
    private boolean isAuthor(Article article, Integer userId) {
        if (article.getAuthorId().equals(userId)) {
            return true;
        }
        return articleAuthorRepository.existsByArticleIdAndUserId(article.getId(), userId);
    }

    /**
     * Doi trang thai bai va ghi 1 dong lich su trong cung transaction (AGENTS.md muc 2).
     * Luong khac (BF-02...) sau nay cung phai doi trang thai qua ham cua ArticleService.
     */
    private void changeStatus(Article article, ArticleStatus newStatus, Integer actorId, String action, String note) {
        LocalDateTime now = LocalDateTime.now();

        ArticleStatusLog log = new ArticleStatusLog();
        log.setArticleId(article.getId());
        log.setActorId(actorId);
        log.setAction(action);
        log.setFromStatus(article.getStatus());
        log.setToStatus(newStatus);
        log.setNote(note);
        log.setCreatedAt(now);
        articleStatusLogRepository.save(log);

        article.setStatus(newStatus);
        article.setUpdatedAt(now);
        articleRepository.save(article);
    }

    // Gom bai + tac gia + lich su (kem ho ten) thanh DTO chi tiet bai (UC-02)
    private ArticleDetailResponse toDetailResponse(Article article, Integer currentUserId) {
        Map<Integer, String> userNames = loadUserNames();

        List<ArticleAuthorResponse> authors = new ArrayList<>();
        for (ArticleAuthor author : articleAuthorRepository.findByArticleIdOrderBySharePercentDesc(article.getId())) {
            authors.add(ArticleAuthorResponse.from(author, article.getAuthorId(), userNames.get(author.getUserId())));
        }

        List<ArticleStatusLogResponse> statusLogs = new ArrayList<>();
        for (ArticleStatusLog log : articleStatusLogRepository.findByArticleIdOrderByCreatedAtAscIdAsc(article.getId())) {
            statusLogs.add(ArticleStatusLogResponse.from(log, userNames.get(log.getActorId())));
        }

        // Nut "Rut bai" chi hien voi tac gia chinh khi bai dang SUBMITTED (BR-IN-08)
        boolean canWithdraw = article.getAuthorId().equals(currentUserId)
                && article.getStatus() == ArticleStatus.SUBMITTED;

        return ArticleDetailResponse.from(article, userNames.get(article.getAuthorId()),
                authors, statusLogs, canWithdraw);
    }

    // Map userId -> ho ten, de doi ma nguoi dung thanh ten khi tra ve giao dien
    private Map<Integer, String> loadUserNames() {
        Map<Integer, String> names = new HashMap<>();
        for (UserAccessResponse user : userReadService.getUsers()) {
            names.put(user.id(), user.fullName());
        }
        return names;
    }
}
