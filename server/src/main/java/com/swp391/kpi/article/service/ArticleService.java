package com.swp391.kpi.article.service;

import com.swp391.kpi.admin.dto.GenreResponse;
import com.swp391.kpi.admin.dto.UserAccessResponse;
import com.swp391.kpi.admin.service.GenreService;
import com.swp391.kpi.admin.service.UserReadService;
import com.swp391.kpi.article.dto.ArticleAuthorResponse;
import com.swp391.kpi.article.dto.ArticleAuthorShareResponse;
import com.swp391.kpi.article.dto.ArticleCoAuthorInviteResponse;
import com.swp391.kpi.article.dto.ArticleDetailResponse;
import com.swp391.kpi.article.dto.ArticlePageResponse;
import com.swp391.kpi.article.dto.ArticleReadResponse;
import com.swp391.kpi.article.dto.ArticleRevisionRequestResponse;
import com.swp391.kpi.article.dto.ArticleStatusCountResponse;
import com.swp391.kpi.article.dto.ArticleStatusLogResponse;
import com.swp391.kpi.article.dto.ArticleSummaryResponse;
import com.swp391.kpi.article.dto.ArticleVersionReadResponse;
import com.swp391.kpi.article.entity.Article;
import com.swp391.kpi.article.entity.ArticleAuthor;
import com.swp391.kpi.article.entity.ArticleAuthorConfirmStatus;
import com.swp391.kpi.article.entity.ArticleStatus;
import com.swp391.kpi.article.entity.ArticleStatusLog;
import com.swp391.kpi.article.entity.ArticleVersion;
import com.swp391.kpi.article.repository.ArticleAuthorRepository;
import com.swp391.kpi.article.repository.ArticleRepository;
import com.swp391.kpi.article.repository.ArticleStatusLogRepository;
import com.swp391.kpi.article.repository.ArticleVersionRepository;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
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
 * MUC LUC (so dong - ten - tac dung)
 *   83  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   90  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *  104  getMyArticles()                 UC-01: bai ma nguoi dung la tac gia chinh hoac dong tac gia, sua gan nhat len dau
 *  151  getArticleDetail()              UC-02: xem noi dung, khai bao, tac gia va lich su trang thai cua 1 bai
 *  168  buildArticleDetail()            UC-02 cho nguoi xem khong phai tac gia (BTV duoc giao, Truong ban): dung chi tiet bai, KHONG kiem quyen
 *  177  withdrawArticle()               UC-11: tac gia chinh rut bai da nop khi chua phan cong BTV, bai ve DRAFT (BR-IN-08)
 *  206  publishArticle()                Cho BF-02 (UC-33 duyet dang): bai SCORE_PUBLISHED -> PUBLISHED
 *  221  getMyStatusCounts()             Trang chu Phong vien: so bai cua nguoi dung o tung trang thai
 *  242  getMyRevisionRequests()         Trang chu Phong vien: bai cua nguoi dung (tac gia chinh) bi tra ve can sua, kem ghi chu cua lan tra ve gan nh…
 *  262  getMyCoAuthorInvites()          Trang chu Phong vien: loi moi dong tac gia dang cho nguoi dung tra loi (UC-12), sap het han xep truoc
 *  286  isAuthorOrCoAuthor()            Cho BF-02 (Duong, 02/11): nguoi dung la tac gia chinh hoac dong tac gia cua bai thi tra true
 *  312  getVersionForRead()             1 ban luu cua bai (article_versions), dung de cham mu / cham lai dung ban da nop
 *  323  getPublishedByAuthor()          Bai da cong bo diem hoac da dang cua 1 tac gia chinh
 *  334  getArticleForRead()             Thong tin ngan cua 1 bai (tac gia, ban, tieu de, trang thai); khong co bai -> 404
 *  340  getAuthors()                    Moi tac gia cua bai kem ty le nhuan but, ty le cao truoc
 *  350  getAuthorShares()               Ty le nhuan but cua 1 nguoi trong nhieu bai (bai nguoi do khong co ten thi khong co dong)
 *  359  existsArticle()                 kiem tra ton tai article
 *  368  changeCategory()                M-A4: BF-02 doi the loai ap dung cua bai (UC-23)
 *  380  updateArticleStatus()           Ham dung chung cho cac luong khac (BF-02, BF-03, BF-04) doi trang thai bai viet va tu dong ghi 1 dong lich su…
 *  ---- Ham phu (private, chi dung trong file) ----
 *  294  findArticle()                   Tim bai theo ma, khong co thi bao 404 (NotFoundException)
 *  300  isAuthor()                      Tac gia chinh (articles.author_id) hoac co dong trong article_authors
 *  385  changeStatus()                  doi status
 *  404  toReadResponse()                Bai -> DTO doc ngan cho luong khac (M-D2)
 *  410  toDetailResponse()              Gom bai + tac gia + lich su (kem ho ten) thanh DTO chi tiet bai (UC-02)
 *  455  loadGenreNames()                Map genreId -> ten the loai (chi the loai dang dung; the loai da tat -> khong co ten)
 *  464  loadUserNames()                 Map userId -> ho ten, de doi ma nguoi dung thanh ten khi tra ve giao dien
 */
@Service
@RequiredArgsConstructor
public class ArticleService {

    // So dong toi da 1 trang, tranh client xin qua nhieu 1 lan
    private static final int MAX_PAGE_SIZE = 50;

    // Ten hanh dong ghi vao article_status_logs.action khi rut bai
    private static final String ACTION_WITHDRAW = "WITHDRAW";
    // Ten hanh dong khi Truong ban duyet dang (giong database/data.sql)
    private static final String ACTION_PUBLISH = "PUBLISH";

    private final ArticleRepository articleRepository;
    private final ArticleAuthorRepository articleAuthorRepository;
    private final ArticleStatusLogRepository articleStatusLogRepository;
    private final ArticleVersionRepository articleVersionRepository;
    // Ho ten nguoi dung la du lieu luong admin -> goi service cua ho, khong query bang users (AGENTS.md muc 2)
    private final UserReadService userReadService;
    // Ten the loai la du lieu luong admin -> goi GenreService
    private final GenreService genreService;

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

        Map<Integer, String> genreNames = loadGenreNames();
        List<ArticleSummaryResponse> items = new ArrayList<>();
        for (Article article : articlePage.getContent()) {
            ArticleSummaryResponse item = ArticleSummaryResponse.from(article, myAuthorRows.get(article.getId()), currentUserId);
            item.setCategoryName(genreNames.get(article.getCategoryId()));
            items.add(item);
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

        // Chi tac gia chinh va dong tac gia (BR-IN-01: dong tac gia chi xem). Ham nay de cac service BF-01
        // tra lai bai sau khi luu / nop / xac nhan; API GET /api/articles/{id} kiem quyen o ArticleViewService
        if (!isAuthor(article, currentUserId)) {
            throw new BusinessException("Bạn không phải tác giả của bài viết này nên không được xem.");
        }
        return toDetailResponse(article, currentUserId);
    }

    /**
     * UC-02 cho nguoi xem khong phai tac gia (BTV duoc giao, Truong ban): dung chi tiet bai, KHONG kiem quyen.
     * Chi goi tu ArticleViewService sau khi da kiem quyen (ArticleService khong goi duoc AssignmentService
     * vi AssignmentService da goi ArticleService -> vong phu thuoc).
     */
    @Transactional(readOnly = true)
    public ArticleDetailResponse buildArticleDetail(Integer articleId, Integer viewerId) {
        Article article = findArticle(articleId);
        return toDetailResponse(article, viewerId);
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
        // B11 (Quang 01/11): bai SUBMITTED chua tung phan cong BTV -> bo su kien + hang + ty le da khoa,
        // nop lai thi chon lai su kien va dem hang moi. Hang cua bai khac khong tinh lai
        article.setEventId(null);
        article.setSaturationRank(null);
        article.setSaturationRate(null);
        changeStatus(article, ArticleStatus.DRAFT, currentUserId, ACTION_WITHDRAW, null);
        return toDetailResponse(article, currentUserId);
    }

    /**
     * Cho BF-02 (UC-33 duyet dang): bai SCORE_PUBLISHED -> PUBLISHED. Bat buoc ghi published_at
     * va approved_by (CHECK ck_articles_published); BF-04 dung published_at de chon don gia va
     * tinh han chi (GBR-08) - yeu cau cua nhom truong 24/10 (A7).
     */
    @Transactional
    public void publishArticle(Integer articleId, Integer approverId) {
        Article article = findArticle(articleId);
        if (article.getStatus() != ArticleStatus.SCORE_PUBLISHED) {
            throw new BusinessException("Chỉ duyệt đăng được bài đã công bố điểm.");
        }
        article.setPublishedAt(LocalDateTime.now());
        article.setApprovedBy(approverId);
        changeStatus(article, ArticleStatus.PUBLISHED, approverId, ACTION_PUBLISH, null);
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

    /**
     * Cho BF-02 (Duong, 02/11): nguoi dung la tac gia chinh hoac dong tac gia cua bai thi tra true.
     * Dung de kiem quyen xem phieu cham (getLatestAttemptForUser). Bai khong ton tai -> 404.
     */
    @Transactional(readOnly = true)
    public boolean isAuthorOrCoAuthor(Integer articleId, Integer userId) {
        Article article = findArticle(articleId);
        return isAuthor(article, userId);
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

    // ---------------------------------------------------------------- M-D2: ham doc cho luong khac
    // (BF-03, BF-04, BF-05 goi cac ham nay thay vi doc thang bang cua BF-01 bang repository / JdbcTemplate)

    // 1 ban luu cua bai (article_versions), dung de cham mu / cham lai dung ban da nop
    @Transactional(readOnly = true)
    public ArticleVersionReadResponse getVersionForRead(Integer articleId, Integer versionId) {
        ArticleVersion version = articleVersionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản bài viết."));
        if (!version.getArticleId().equals(articleId)) {
            throw new NotFoundException("Không tìm thấy phiên bản bài viết.");
        }
        return new ArticleVersionReadResponse(version.getId(), version.getArticleId(), version.getTitle(), version.getContent());
    }

    // Bai da cong bo diem hoac da dang cua 1 tac gia chinh
    @Transactional(readOnly = true)
    public List<ArticleReadResponse> getPublishedByAuthor(Integer authorId) {
        List<ArticleReadResponse> result = new ArrayList<>();
        for (Article article : articleRepository.findByAuthorIdAndStatusIn(authorId,
                List.of(ArticleStatus.SCORE_PUBLISHED, ArticleStatus.PUBLISHED))) {
            result.add(toReadResponse(article));
        }
        return result;
    }

    // Thong tin ngan cua 1 bai (tac gia, ban, tieu de, trang thai); khong co bai -> 404
    @Transactional(readOnly = true)
    public ArticleReadResponse getArticleForRead(Integer articleId) {
        return toReadResponse(findArticle(articleId));
    }

    // Moi tac gia cua bai kem ty le nhuan but, ty le cao truoc
    @Transactional(readOnly = true)
    public List<ArticleAuthorShareResponse> getAuthors(Integer articleId) {
        List<ArticleAuthorShareResponse> result = new ArrayList<>();
        for (ArticleAuthor author : articleAuthorRepository.findByArticleIdOrderBySharePercentDesc(articleId)) {
            result.add(new ArticleAuthorShareResponse(author.getArticleId(), author.getUserId(), author.getSharePercent()));
        }
        return result;
    }

    // Ty le nhuan but cua 1 nguoi trong nhieu bai (bai nguoi do khong co ten thi khong co dong)
    @Transactional(readOnly = true)
    public List<ArticleAuthorShareResponse> getAuthorShares(Integer userId, List<Integer> articleIds) {
        List<ArticleAuthorShareResponse> result = new ArrayList<>();
        for (ArticleAuthor author : articleAuthorRepository.findByUserIdAndArticleIdIn(userId, articleIds)) {
            result.add(new ArticleAuthorShareResponse(author.getArticleId(), author.getUserId(), author.getSharePercent()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public boolean existsArticle(Integer articleId) {
        return articleRepository.existsById(articleId);
    }

    /**
     * M-A4: BF-02 doi the loai ap dung cua bai (UC-23). Chi doi articles.category_id;
     * dong log category_change_logs do BF-02 ghi (bang cua BF-02, comment bang articles trong schema.sql).
     */
    @Transactional
    public void changeCategory(Integer articleId, Integer newCategoryId) {
        Article article = findArticle(articleId);
        article.setCategoryId(newCategoryId);
        article.setUpdatedAt(LocalDateTime.now());
        articleRepository.save(article);
    }

    /**
     * Ham dung chung cho cac luong khac (BF-02, BF-03, BF-04) doi trang thai bai viet
     * va tu dong ghi 1 dong lich su article_status_logs (AGENTS.md muc 2).
     */
    @Transactional
    public void updateArticleStatus(Integer articleId, ArticleStatus newStatus, Integer actorId, String action, String note) {
        Article article = findArticle(articleId);
        changeStatus(article, newStatus, actorId, action, note);
    }

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

    // Bai -> DTO doc ngan cho luong khac (M-D2)
    private ArticleReadResponse toReadResponse(Article article) {
        return new ArticleReadResponse(article.getId(), article.getAuthorId(), article.getSectionId(),
                article.getTitle(), article.getStatus().name());
    }

    // Gom bai + tac gia + lich su (kem ho ten) thanh DTO chi tiet bai (UC-02)
    private ArticleDetailResponse toDetailResponse(Article article, Integer currentUserId) {
        Map<Integer, String> userNames = loadUserNames();

        List<ArticleAuthorResponse> authors = new ArrayList<>();
        ArticleAuthor myRow = null;   // dong cua nguoi dang xem, de biet co duoc tra loi xac nhan khong
        for (ArticleAuthor author : articleAuthorRepository.findByArticleIdOrderBySharePercentDesc(article.getId())) {
            authors.add(ArticleAuthorResponse.from(author, article.getAuthorId(), userNames.get(author.getUserId())));
            if (author.getUserId().equals(currentUserId)) {
                myRow = author;
            }
        }

        List<ArticleStatusLogResponse> statusLogs = new ArrayList<>();
        for (ArticleStatusLog log : articleStatusLogRepository.findByArticleIdOrderByCreatedAtAscIdAsc(article.getId())) {
            statusLogs.add(ArticleStatusLogResponse.from(log, userNames.get(log.getActorId())));
        }

        // Nut "Rut bai" chi hien voi tac gia chinh khi bai dang SUBMITTED (BR-IN-08)
        boolean canWithdraw = article.getAuthorId().equals(currentUserId)
                && article.getStatus() == ArticleStatus.SUBMITTED;

        ArticleDetailResponse dto = ArticleDetailResponse.from(article, userNames.get(article.getAuthorId()),
                authors, statusLogs, canWithdraw);

        Map<Integer, String> genreNames = loadGenreNames();
        dto.setDeclaredCategoryName(genreNames.get(article.getDeclaredCategoryId()));
        dto.setCategoryName(genreNames.get(article.getCategoryId()));

        // BR-IN-01: chi tac gia chinh sua, khi bai DRAFT hoac REVISION_REQUESTED
        boolean isMainAuthor = article.getAuthorId().equals(currentUserId);
        dto.setCanEdit(isMainAuthor && (article.getStatus() == ArticleStatus.DRAFT
                || article.getStatus() == ArticleStatus.REVISION_REQUESTED));
        dto.setCanSubmit(isMainAuthor && article.getStatus() == ArticleStatus.DRAFT);
        dto.setCanResubmit(isMainAuthor && article.getStatus() == ArticleStatus.REVISION_REQUESTED);

        // UC-12 Preconditions: bai PENDING_CONFIRMATION, minh la dong tac gia dang PENDING, chua qua han
        dto.setCanRespond(!isMainAuthor && myRow != null
                && article.getStatus() == ArticleStatus.PENDING_CONFIRMATION
                && myRow.getConfirmStatus() == ArticleAuthorConfirmStatus.PENDING
                && myRow.getConfirmDeadline() != null
                && myRow.getConfirmDeadline().isAfter(LocalDateTime.now()));
        return dto;
    }

    // Map genreId -> ten the loai (chi the loai dang dung; the loai da tat -> khong co ten)
    private Map<Integer, String> loadGenreNames() {
        Map<Integer, String> names = new HashMap<>();
        for (GenreResponse genre : genreService.getActiveGenres()) {
            names.put(genre.getId(), genre.getName());
        }
        return names;
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
