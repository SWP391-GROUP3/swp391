package com.swp391.kpi.service;

import com.swp391.kpi.dto.ArticleAuthorShareRequest;
import com.swp391.kpi.dto.ArticleCoAuthorCandidateResponse;
import com.swp391.kpi.dto.ArticleDetailResponse;
import com.swp391.kpi.dto.ArticleRoyaltyEstimateRequest;
import com.swp391.kpi.dto.ArticleSaveRequest;
import com.swp391.kpi.dto.AssignmentResponse;
import com.swp391.kpi.dto.DuplicationCheckResponse;
import com.swp391.kpi.dto.EventResponse;
import com.swp391.kpi.dto.GenreResponse;
import com.swp391.kpi.dto.RoyaltyEstimateResponse;
import com.swp391.kpi.dto.UserAccessResponse;
import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleAuthor;
import com.swp391.kpi.entity.ArticleAuthorConfirmStatus;
import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.entity.ArticleStatusLog;
import com.swp391.kpi.entity.ArticleVersion;
import com.swp391.kpi.entity.ArticleVersionChangeType;
import com.swp391.kpi.entity.ArticleVersionKind;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.ArticleAuthorRepository;
import com.swp391.kpi.repository.ArticleRepository;
import com.swp391.kpi.repository.ArticleStatusLogRepository;
import com.swp391.kpi.repository.ArticleVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Work Editor BF-01 Iter 2: soan / sua nhap (UC-03), khai dong tac gia + ty le (UC-04),
 * nop bai (UC-05), sua va nop lai (UC-10).
 * Doi articles.status luon goi ArticleService.updateArticleStatus() de ghi kem article_status_logs.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   79  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   92  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *  114  getCoAuthorCandidates()         UC-04: phong vien co the chon lam dong tac gia (Phong vien dang ACTIVE, tru chinh minh)
 *  129  createDraft()                   UC-03 + UC-04: tao ban nhap moi, nguoi tao la tac gia chinh
 *  181  updateDraft()                   UC-03 + UC-04: sua ban nhap
 *  209  submitArticle()                 UC-05: nop bai DRAFT. Bai da tung phan cong BTV (vd dot xac nhan khi nop lai bi tu choi, bai ve DRAFT) van ti…
 *  224  resubmitArticle()               UC-10: nop lai bai bi tra ve
 *  240  estimateRoyalty()               UC-09: uoc tinh nhuan but phan bai viet truoc khi nop, khong luu gi
 *  ---- Ham phu (private, chi dung trong file) ----
 *  270  doSubmit()                      Phan chung cua nop / nop lai: kiem du lieu -> kiem trung noi dung (chan neu can) -> chup ban SUBMISSION -> lu…
 *  379  saveAuthors()                   Luu danh sach tac gia: sua tai cho (khong xoa roi them lai cung khoa chinh)
 *  422  copyDeclarations()              Chep khai bao tu request vao bai
 *  436  authorsOrMainOnly()             Khong gui danh sach tac gia -> chi tac gia chinh 100%
 *  447  validateShares()                BR-IN-02 + dong tac gia phai la Phong vien dang ACTIVE (UC-04 buoc 2 "selects a reporter")
 *  472  shareErrorOfRows()              Kiem lai ty le tren cac dong da luu (luc nop)
 *  487  lockSaturation()                Khoa hang + ty le bao hoa luc nop lan dau (BR-IN-03)
 *  500  findOpenEvent()                 B14: su kien phai con mo luc nop (EventService.getOpenEvents: dang bat, hom nay trong khoang ngay)
 *  515  countSaturationRank()           B10 (Quang 01/11): hang bao hoa cua bai luc nop = so bai khac cung su kien da khoa hang, co lan nop (log SUBM…
 *  532  checkCategory()                 The loai phai la the loai dang dung (GenreService cua luong admin)
 *  542  checkMainAuthor()               BR-IN-01: chi tac gia chinh (articles.author_id) duoc sua / nop / nop lai
 *  549  findArticle()                   Tim bai theo ma, khong co thi bao 404 (NotFoundException)
 *  555  findUser()                      Tim tai khoan qua UserReadService (luong admin), khong co thi 404
 *  565  readIntParam()                  Tham so so nguyen cua BF-01 trong system_parameters, khong viet cung so trong code
 *  574  shorten()                       notifications.message toi da 500 ky tu (tieu de bai co the dai toi 300)
 */
@Service
@RequiredArgsConstructor
public class ArticleEditorService {

    // Ten tham so trong system_parameters (flow BF-01, xem database/data.sql)
    private static final String PARAM_MAX_AUTHORS = "MAX_AUTHORS_PER_ARTICLE";
    private static final String PARAM_CONFIRM_DAYS = "CO_AUTHOR_CONFIRM_DAYS";

    // Ten hanh dong ghi vao article_status_logs.action (giong database/data.sql)
    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_SUBMIT = "SUBMIT";
    private static final String ACTION_RESUBMIT = "RESUBMIT";

    private static final String ROLE_REPORTER = "REPORTER";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final int MAX_MESSAGE_LENGTH = 500;   // notifications.message NVARCHAR(500)
    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ArticleRepository articleRepository;
    private final ArticleAuthorRepository articleAuthorRepository;
    private final ArticleStatusLogRepository articleStatusLogRepository;
    private final ArticleVersionRepository articleVersionRepository;
    private final ArticleService articleService;
    // Du lieu luong khac -> goi service cua ho (AGENTS.md muc 2)
    private final UserReadService userReadService;
    private final GenreService genreService;
    private final SystemParameterService systemParameterService;
    private final AssignmentService assignmentService;
    private final NotificationService notificationService;
    private final EventService eventService;
    private final RoyaltyGradingService royaltyGradingService;
    // Iter 3: kiem trung anh/video + gan vao ban nop (UC-07)
    private final ArticleAttachmentService articleAttachmentService;
    // Iter 3: kiem trung noi dung + giai trinh (UC-06, UC-08)
    private final DuplicationCheckService duplicationCheckService;

    /**
     * UC-04: phong vien co the chon lam dong tac gia (Phong vien dang ACTIVE, tru chinh minh).
     */
    @Transactional(readOnly = true)
    public List<ArticleCoAuthorCandidateResponse> getCoAuthorCandidates(Integer currentUserId) {
        List<ArticleCoAuthorCandidateResponse> result = new ArrayList<>();
        for (UserAccessResponse user : userReadService.getUsers()) {
            if (ROLE_REPORTER.equals(user.role()) && STATUS_ACTIVE.equals(user.status())
                    && !user.id().equals(currentUserId)) {
                result.add(ArticleCoAuthorCandidateResponse.from(user));
            }
        }
        return result;
    }

    /**
     * UC-03 + UC-04: tao ban nhap moi, nguoi tao la tac gia chinh. Ghi 1 dong log CREATE.
     */
    @Transactional
    public ArticleDetailResponse createDraft(Integer currentUserId, ArticleSaveRequest request) {
        UserAccessResponse me = findUser(currentUserId);
        // Comment bang articles: section_id = ban cua tac gia chinh TAI LUC TAO BAI
        if (me.sectionId() == null) {
            throw new BusinessException("Tài khoản của bạn chưa thuộc ban nào nên chưa tạo được bài.");
        }
        checkCategory(request.getDeclaredCategoryId());
        List<ArticleAuthorShareRequest> authors = authorsOrMainOnly(request, currentUserId);
        validateShares(currentUserId, authors);

        LocalDateTime now = LocalDateTime.now();
        Article article = new Article();
        article.setAuthorId(currentUserId);
        article.setSectionId(me.sectionId());
        article.setStatus(ArticleStatus.DRAFT);
        // event_id chep trong copyDeclarations; saturation_rank / saturation_rate chi khoa luc nop (BR-IN-03)
        article.setCreatedAt(now);
        copyDeclarations(article, request, true);
        articleRepository.save(article);

        // Tac gia chinh ACCEPTED ngay, dong tac gia PENDING nhung chua bao toi luc nop (UC-04 Postconditions)
        for (ArticleAuthorShareRequest item : authors) {
            ArticleAuthor row = new ArticleAuthor();
            row.setArticleId(article.getId());
            row.setUserId(item.getUserId());
            row.setSharePercent(item.getSharePercent());
            if (item.getUserId().equals(currentUserId)) {
                row.setConfirmStatus(ArticleAuthorConfirmStatus.ACCEPTED);
                row.setRespondedAt(now);
            } else {
                row.setConfirmStatus(ArticleAuthorConfirmStatus.PENDING);
            }
            articleAuthorRepository.save(row);
        }

        // Lan tao bai: from_status NULL (comment bang article_status_logs) nen ghi thang, khong qua doi trang thai
        ArticleStatusLog log = new ArticleStatusLog();
        log.setArticleId(article.getId());
        log.setActorId(currentUserId);
        log.setAction(ACTION_CREATE);
        log.setToStatus(ArticleStatus.DRAFT);
        log.setCreatedAt(now);
        articleStatusLogRepository.save(log);

        return articleService.getArticleDetail(article.getId(), currentUserId);
    }

    /**
     * UC-03 + UC-04: sua ban nhap. Chi tac gia chinh, chi khi DRAFT / REVISION_REQUESTED (BR-IN-01).
     * Doi danh sach tac gia hoac ty le -> moi dong tac gia ve PENDING (nhom truong 24/10, B8).
     */
    @Transactional
    public ArticleDetailResponse updateDraft(Integer articleId, Integer currentUserId, ArticleSaveRequest request) {
        Article article = findArticle(articleId);
        checkMainAuthor(article, currentUserId, "Chỉ tác giả chính mới được sửa bài.");
        if (article.getStatus() != ArticleStatus.DRAFT && article.getStatus() != ArticleStatus.REVISION_REQUESTED) {
            throw new BusinessException("Chỉ sửa được bài ở trạng thái Nháp hoặc Trả về – cần sửa.");
        }
        checkCategory(request.getDeclaredCategoryId());
        List<ArticleAuthorShareRequest> authors = authorsOrMainOnly(request, currentUserId);
        validateShares(currentUserId, authors);

        // Bai da tung phan cong BTV thi category_id la the loai BTV ap dung -> giu nguyen
        boolean everAssigned = assignmentService.getAssignmentByArticleId(articleId).isPresent();
        // B13 (Quang 01/11): bai da tung phan cong BTV thi giu su kien va ty le bao hoa da khoa (BR-IN-07)
        if (everAssigned && !Objects.equals(article.getEventId(), request.getEventId())) {
            throw new BusinessException("Bài đã được phân công Biên tập viên nên không đổi được sự kiện.");
        }
        copyDeclarations(article, request, !everAssigned);
        articleRepository.save(article);

        saveAuthors(article, authors, currentUserId);
        return articleService.getArticleDetail(articleId, currentUserId);
    }

    /**
     * UC-05: nop bai DRAFT. Bai da tung phan cong BTV (vd dot xac nhan khi nop lai bi tu choi,
     * bai ve DRAFT) van tinh la nop lai -> ve dung BTV cu (quyet dinh 12/10, B9).
     */
    @Transactional
    public ArticleDetailResponse submitArticle(Integer articleId, Integer currentUserId, String explanation) {
        Article article = findArticle(articleId);
        checkMainAuthor(article, currentUserId, "Chỉ tác giả chính mới được nộp bài.");
        if (article.getStatus() != ArticleStatus.DRAFT) {
            throw new BusinessException("Chỉ nộp được bài ở trạng thái Nháp. Bài bị trả về thì dùng Nộp lại.");
        }
        doSubmit(article, currentUserId, explanation);
        return articleService.getArticleDetail(articleId, currentUserId);
    }

    /**
     * UC-10: nop lai bai bi tra ve. Chi sua noi dung -> ASSIGNED dung BTV cu, doi tac gia/ty le ->
     * xac nhan lai (BR-IN-07). Bai REJECTED khong vao duoc day (E2).
     */
    @Transactional
    public ArticleDetailResponse resubmitArticle(Integer articleId, Integer currentUserId, String explanation) {
        Article article = findArticle(articleId);
        checkMainAuthor(article, currentUserId, "Chỉ tác giả chính mới được nộp lại bài.");
        if (article.getStatus() != ArticleStatus.REVISION_REQUESTED) {
            throw new BusinessException("Chỉ nộp lại được bài đang ở trạng thái Trả về – cần sửa.");
        }
        doSubmit(article, currentUserId, explanation);
        return articleService.getArticleDetail(articleId, currentUserId);
    }

    /**
     * UC-09: uoc tinh nhuan but phan bai viet truoc khi nop, khong luu gi. Thu tu theo B16 (Quang 01/11):
     * dem hang "neu nop ngay luc nay" (B10) -> doi ra ty le (EventService) -> RoyaltyGradingService.
     * Gui articleId cua bai da tung phan cong BTV -> dung ty le da khoa lan dau (BR-IN-07, quyet dinh 03/11).
     */
    @Transactional(readOnly = true)
    public RoyaltyEstimateResponse estimateRoyalty(Integer currentUserId, ArticleRoyaltyEstimateRequest request) {
        Integer eventId = request.getEventId();
        BigDecimal saturationRate = null;   // null = khong thuoc su kien (BF-04 coi la 100%)

        boolean usesLockedRate = false;
        if (request.getArticleId() != null) {
            Article article = findArticle(request.getArticleId());
            checkMainAuthor(article, currentUserId, "Chỉ tác giả chính mới được ước tính nhuận bút của bài.");
            if (assignmentService.getAssignmentByArticleId(article.getId()).isPresent()) {
                eventId = article.getEventId();
                saturationRate = article.getSaturationRate();
                usesLockedRate = true;
            }
        }
        if (!usesLockedRate && eventId != null) {
            EventResponse event = findOpenEvent(eventId);
            int rank = countSaturationRank(event, request.getArticleId(), LocalDateTime.now());
            saturationRate = eventService.getSaturationRate(eventId, rank);
        }
        return royaltyGradingService.estimateRoyalty(request.getDeclaredCategoryId(),
                request.getDeclaredNature().name(), eventId, saturationRate);
    }

    // ---------------------------------------------------------------- ham phu

    /**
     * Phan chung cua nop / nop lai: kiem du lieu -> kiem trung noi dung (chan neu can) -> chup ban SUBMISSION
     * -> luu ket qua kiem trung + kiem trung anh/video -> dat han xac nhan -> doi trang thai -> bao tin.
     * explanation: giai trinh khi trung tu DUP_WARN_PERCENT (UC-08), co the null.
     */
    private void doSubmit(Article article, Integer currentUserId, String explanation) {
        // UC-05 Preconditions + E2: thieu du lieu thi liet ke het loi, bai giu nguyen trang thai
        List<String> errors = new ArrayList<>();
        if (article.getContent() == null || article.getContent().isBlank()) {
            errors.add("Nội dung bài không được để trống.");
        }
        if (article.getDeclaredNature() == null) {
            errors.add("Chưa chọn tính chất bài.");
        }
        List<ArticleAuthor> rows = articleAuthorRepository.findByArticleIdOrderBySharePercentDesc(article.getId());
        String shareError = shareErrorOfRows(article.getAuthorId(), rows);
        if (shareError != null) {
            errors.add(shareError);
        }
        // BR-IN-05 / B23: tac gia moi anh/video phai con trong danh sach tac gia (co the vua bo dong tac gia)
        String mediaAuthorError = articleAttachmentService.findAuthorError(article.getId(), article.getAuthorId(), rows);
        if (mediaAuthorError != null) {
            errors.add(mediaAuthorError);
        }
        if (!errors.isEmpty()) {
            throw new BusinessException(String.join(" ", errors));
        }

        // UC-06 / BR-IN-04: kiem trung truoc khi xac nhan dong tac gia; BLOCKED hoac thieu giai trinh -> 409, bai giu nguyen
        DuplicationCheckResponse duplication = duplicationCheckService.checkBeforeSubmit(article, explanation);

        LocalDateTime now = LocalDateTime.now();
        Optional<AssignmentResponse> assignment = assignmentService.getAssignmentByArticleId(article.getId());
        boolean isResubmit = assignment.isPresent();   // quyet dinh 12/10: da co dong assignments = nop lai

        // BR-IN-03: nop lan dau thi khoa hang + ty le bao hoa; nop lai giu ty le da khoa (BR-IN-07)
        if (!isResubmit) {
            lockSaturation(article, now);
        }

        // Chup ban SUBMISSION (bang article_versions chi INSERT). Lan dau INITIAL, nop lai MAJOR (B10 - gia dinh)
        boolean hasEarlierSubmission = articleVersionRepository
                .findFirstByArticleIdAndKindOrderByVersionNoDesc(article.getId(), ArticleVersionKind.SUBMISSION)
                .isPresent();
        int nextVersionNo = 1;
        Optional<ArticleVersion> latest = articleVersionRepository.findFirstByArticleIdOrderByVersionNoDesc(article.getId());
        if (latest.isPresent()) {
            nextVersionNo = latest.get().getVersionNo() + 1;
        }
        ArticleVersion version = new ArticleVersion();
        version.setArticleId(article.getId());
        version.setVersionNo(nextVersionNo);
        version.setKind(ArticleVersionKind.SUBMISSION);
        version.setTitle(article.getTitle());
        version.setContent(article.getContent());
        version.setChangeType(hasEarlierSubmission ? ArticleVersionChangeType.MAJOR : ArticleVersionChangeType.INITIAL);
        version.setCreatedBy(currentUserId);
        version.setCreatedAt(now);
        articleVersionRepository.save(version);

        // UC-07: kiem trung anh/video, roi gan tat ca anh/video cua bai vao ban SUBMISSION nay (quyet dinh 03/11)
        articleAttachmentService.checkAndAttachToSubmission(article.getId(), version.getId());

        // B5: luu ket qua kiem trung noi dung gan voi ban nop vua tao (version_id NOT NULL)
        duplicationCheckService.saveResult(article.getId(), version.getId(), duplication, explanation, now);

        article.setSubmittedAt(now);

        // Dong tac gia can xac nhan: lan nop dau -> tat ca; nop lai -> nguoi chua ACCEPTED
        // (doi tac gia/ty le luc luu nhap da dua ho ve PENDING - B8). Han = luc nop + 3 ngay (BR-IN-06)
        LocalDateTime deadline = now.plusDays(readIntParam(PARAM_CONFIRM_DAYS));
        List<ArticleAuthor> needConfirm = new ArrayList<>();
        for (ArticleAuthor row : rows) {
            if (row.getUserId().equals(article.getAuthorId())) {
                continue;
            }
            if (!isResubmit || row.getConfirmStatus() != ArticleAuthorConfirmStatus.ACCEPTED) {
                row.setConfirmStatus(ArticleAuthorConfirmStatus.PENDING);
                row.setConfirmDeadline(deadline);
                row.setRespondedAt(null);
                row.setRejectReason(null);
                articleAuthorRepository.save(row);
                needConfirm.add(row);
            }
        }

        String action = isResubmit ? ACTION_RESUBMIT : ACTION_SUBMIT;
        if (!needConfirm.isEmpty()) {
            // UC-05 buoc 5: co dong tac gia -> PENDING_CONFIRMATION, bao tung nguoi
            articleService.updateArticleStatus(article.getId(), ArticleStatus.PENDING_CONFIRMATION,
                    currentUserId, action, null);
            for (ArticleAuthor row : needConfirm) {
                notificationService.notifyUser(row.getUserId(), article.getId(), shorten(
                        "Bạn được mời làm đồng tác giả bài \"" + article.getTitle() + "\" với tỷ lệ "
                                + row.getSharePercent().stripTrailingZeros().toPlainString()
                                + "%. Hạn xác nhận: " + deadline.format(DEADLINE_FORMAT) + "."));
            }
        } else if (isResubmit) {
            // BR-IN-07: chi sua noi dung -> ve dung BTV cu
            articleService.updateArticleStatus(article.getId(), ArticleStatus.ASSIGNED, currentUserId, action, null);
            notificationService.notifyUser(assignment.get().getEditorId(), article.getId(), shorten(
                    "Bài \"" + article.getTitle() + "\" đã được tác giả sửa và nộp lại."));
        } else {
            // UC-05 A2: bai 1 tac gia -> SUBMITTED, bao Truong ban
            articleService.updateArticleStatus(article.getId(), ArticleStatus.SUBMITTED, currentUserId, action, null);
            notificationService.notifyHeads(article.getId(), article.getSectionId(), shorten(
                    "Bài \"" + article.getTitle() + "\" vừa được nộp, chờ phân công biên tập viên."));
        }
    }

    /**
     * Luu danh sach tac gia: sua tai cho (khong xoa roi them lai cung khoa chinh).
     * Danh sach hoac ty le khac ban dang luu -> moi dong tac gia ve PENDING (B8).
     */
    private void saveAuthors(Article article, List<ArticleAuthorShareRequest> authors, Integer mainAuthorId) {
        Map<Integer, ArticleAuthor> existing = new HashMap<>();
        for (ArticleAuthor row : articleAuthorRepository.findByArticleIdOrderBySharePercentDesc(article.getId())) {
            existing.put(row.getUserId(), row);
        }

        boolean changed = existing.size() != authors.size();
        for (ArticleAuthorShareRequest item : authors) {
            ArticleAuthor old = existing.get(item.getUserId());
            if (old == null || old.getSharePercent().compareTo(item.getSharePercent()) != 0) {
                changed = true;
            }
        }

        List<Integer> keepIds = new ArrayList<>();
        for (ArticleAuthorShareRequest item : authors) {
            keepIds.add(item.getUserId());
            ArticleAuthor row = existing.get(item.getUserId());
            if (row == null) {
                row = new ArticleAuthor();
                row.setArticleId(article.getId());
                row.setUserId(item.getUserId());
                row.setConfirmStatus(ArticleAuthorConfirmStatus.PENDING);
            }
            row.setSharePercent(item.getSharePercent());
            if (changed && !item.getUserId().equals(mainAuthorId)) {
                row.setConfirmStatus(ArticleAuthorConfirmStatus.PENDING);
                row.setConfirmDeadline(null);
                row.setRespondedAt(null);
                row.setRejectReason(null);
            }
            articleAuthorRepository.save(row);
        }

        // Nguoi bi bo khoi danh sach (UC-04 A1)
        for (ArticleAuthor row : existing.values()) {
            if (!keepIds.contains(row.getUserId())) {
                articleAuthorRepository.delete(row);
            }
        }
    }

    // Chep khai bao tu request vao bai. updateAppliedCategory = false khi BTV da ap the loai
    private void copyDeclarations(Article article, ArticleSaveRequest request, boolean updateAppliedCategory) {
        article.setTitle(request.getTitle().trim());
        article.setContent(request.getContent());
        article.setDeclaredCategoryId(request.getDeclaredCategoryId());
        if (updateAppliedCategory) {
            // Comment bang articles: luc tao, the loai ap dung = the loai PV khai
            article.setCategoryId(request.getDeclaredCategoryId());
        }
        article.setDeclaredNature(request.getDeclaredNature());
        article.setEventId(request.getEventId());
        article.setUpdatedAt(LocalDateTime.now());
    }

    // Khong gui danh sach tac gia -> chi tac gia chinh 100%
    private List<ArticleAuthorShareRequest> authorsOrMainOnly(ArticleSaveRequest request, Integer mainAuthorId) {
        if (request.getAuthors() != null && !request.getAuthors().isEmpty()) {
            return request.getAuthors();
        }
        ArticleAuthorShareRequest main = new ArticleAuthorShareRequest();
        main.setUserId(mainAuthorId);
        main.setSharePercent(new BigDecimal("100"));
        return List.of(main);
    }

    // BR-IN-02 + dong tac gia phai la Phong vien dang ACTIVE (UC-04 buoc 2 "selects a reporter")
    private void validateShares(Integer mainAuthorId, List<ArticleAuthorShareRequest> authors) {
        List<Integer> userIds = new ArrayList<>();
        List<BigDecimal> shares = new ArrayList<>();
        for (ArticleAuthorShareRequest item : authors) {
            userIds.add(item.getUserId());
            shares.add(item.getSharePercent());
        }
        String error = new ArticleShareValidator(readIntParam(PARAM_MAX_AUTHORS)).findError(mainAuthorId, userIds, shares);
        if (error != null) {
            throw new BusinessException(error);
        }

        Map<Integer, UserAccessResponse> users = new HashMap<>();
        for (UserAccessResponse user : userReadService.getUsers()) {
            users.put(user.id(), user);
        }
        for (Integer userId : userIds) {
            UserAccessResponse user = users.get(userId);
            if (user == null || !ROLE_REPORTER.equals(user.role()) || !STATUS_ACTIVE.equals(user.status())) {
                throw new BusinessException("Đồng tác giả phải là phóng viên đang hoạt động (mã " + userId + ").");
            }
        }
    }

    // Kiem lai ty le tren cac dong da luu (luc nop)
    private String shareErrorOfRows(Integer mainAuthorId, List<ArticleAuthor> rows) {
        List<Integer> userIds = new ArrayList<>();
        List<BigDecimal> shares = new ArrayList<>();
        for (ArticleAuthor row : rows) {
            userIds.add(row.getUserId());
            shares.add(row.getSharePercent());
        }
        return new ArticleShareValidator(readIntParam(PARAM_MAX_AUTHORS)).findError(mainAuthorId, userIds, shares);
    }

    /**
     * Khoa hang + ty le bao hoa luc nop lan dau (BR-IN-03). Thu tu theo B16 (Quang 01/11):
     * kiem su kien con mo (B14) -> dem hang (B10) -> doi hang ra ty le (EventService, GBR-09).
     * Khong chon su kien -> de NULL (comment bang articles; BF-04 coi NULL = 100%).
     */
    private void lockSaturation(Article article, LocalDateTime submitTime) {
        if (article.getEventId() == null) {
            article.setSaturationRank(null);
            article.setSaturationRate(null);
            return;
        }
        EventResponse event = findOpenEvent(article.getEventId());
        int rank = countSaturationRank(event, article.getId(), submitTime);
        article.setSaturationRank(rank);
        article.setSaturationRate(eventService.getSaturationRate(event.getId(), rank));
    }

    // B14: su kien phai con mo luc nop (EventService.getOpenEvents: dang bat, hom nay trong khoang ngay)
    private EventResponse findOpenEvent(Integer eventId) {
        for (EventResponse event : eventService.getOpenEvents()) {
            if (event.getId().equals(eventId)) {
                return event;
            }
        }
        throw new BusinessException("Sự kiện đã kết thúc, vui lòng chọn lại.");
    }

    /**
     * B10 (Quang 01/11): hang bao hoa cua bai luc nop = so bai khac cung su kien da khoa hang,
     * co lan nop (log SUBMIT moi nhat - quyet dinh 03/11) trong windowDays ngay truoc luc nop, cong 1.
     * Bai REJECTED van giu hang nen van duoc dem (B12); bai rut ve DRAFT da xoa hang nen khong dem (B11).
     * articleId = bai dang xet (bo khoi so dem); null khi uoc tinh cho bai chua luu (UC-09).
     */
    private int countSaturationRank(EventResponse event, Integer articleId, LocalDateTime submitTime) {
        LocalDateTime windowStart = submitTime.minusDays(event.getWindowDays());
        int count = 0;
        for (Article other : articleRepository.findByEventIdAndSaturationRankIsNotNull(event.getId())) {
            if (other.getId().equals(articleId)) {
                continue;
            }
            Optional<ArticleStatusLog> lastSubmit = articleStatusLogRepository
                    .findFirstByArticleIdAndActionOrderByCreatedAtDescIdDesc(other.getId(), ACTION_SUBMIT);
            if (lastSubmit.isPresent() && !lastSubmit.get().getCreatedAt().isBefore(windowStart)) {
                count++;
            }
        }
        return count + 1;
    }

    // The loai phai la the loai dang dung (GenreService cua luong admin)
    private void checkCategory(Integer categoryId) {
        for (GenreResponse genre : genreService.getActiveGenres()) {
            if (genre.getId().equals(categoryId)) {
                return;
            }
        }
        throw new BusinessException("Thể loại đã chọn không tồn tại hoặc đã ngừng sử dụng.");
    }

    // BR-IN-01: chi tac gia chinh (articles.author_id) duoc sua / nop / nop lai
    private void checkMainAuthor(Article article, Integer currentUserId, String message) {
        if (!article.getAuthorId().equals(currentUserId)) {
            throw new BusinessException(message);
        }
    }

    // Tim bai theo ma, khong co thi bao 404 (NotFoundException)
    private Article findArticle(Integer articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy bài viết có mã " + articleId + "."));
    }

    // Tim tai khoan qua UserReadService (luong admin), khong co thi 404
    private UserAccessResponse findUser(Integer userId) {
        for (UserAccessResponse user : userReadService.getUsers()) {
            if (user.id().equals(userId)) {
                return user;
            }
        }
        throw new NotFoundException("Không tìm thấy tài khoản.");
    }

    // Tham so so nguyen cua BF-01 trong system_parameters, khong viet cung so trong code
    private int readIntParam(String key) {
        String value = systemParameterService.getValuesByFlow("BF-01").get(key);
        if (value == null) {
            throw new BusinessException("Thiếu tham số hệ thống " + key + ".");
        }
        return Integer.parseInt(value.trim());
    }

    // notifications.message toi da 500 ky tu (tieu de bai co the dai toi 300)
    private String shorten(String message) {
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_MESSAGE_LENGTH - 1) + "…";
    }
}
