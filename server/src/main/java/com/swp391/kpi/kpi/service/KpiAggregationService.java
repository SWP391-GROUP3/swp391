package com.swp391.kpi.kpi.service;
import com.swp391.kpi.admin.entity.RoleCode;
import com.swp391.kpi.admin.entity.User;
import com.swp391.kpi.admin.repository.UserRepository;
import com.swp391.kpi.appeal.service.AppealEffectiveScoreService;
import com.swp391.kpi.article.entity.Article;
import com.swp391.kpi.article.entity.ArticleAuthor;
import com.swp391.kpi.article.entity.ArticleStatus;
import com.swp391.kpi.article.repository.ArticleAuthorRepository;
import com.swp391.kpi.article.repository.ArticleRepository;
import com.swp391.kpi.common.service.AuditService;
import com.swp391.kpi.common.service.NotificationService;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.kpi.dto.DepartmentKpiDashboardResponse;
import com.swp391.kpi.kpi.dto.DepartmentKpiItemResponse;
import com.swp391.kpi.kpi.dto.KpiFinalizeResponse;
import com.swp391.kpi.kpi.dto.KpiSnapshotResponse;
import com.swp391.kpi.kpi.dto.ReporterKpiArticleItemResponse;
import com.swp391.kpi.kpi.dto.ReporterKpiDetailResponse;
import com.swp391.kpi.kpi.entity.KpiSnapshot;
import com.swp391.kpi.kpi.entity.KpiSnapshotStatus;
import com.swp391.kpi.kpi.entity.KpiTarget;
import com.swp391.kpi.kpi.repository.KpiSnapshotRepository;
import com.swp391.kpi.kpi.repository.KpiTargetRepository;
import com.swp391.kpi.kpi.repository.ReaderInteractionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service thuc hien tong hop KPI tu dong (Aggregation Job) va cung cap
 * du lieu cho bang KPI toan ban (Department KPI Board - UC-61).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   63  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   64  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   83  aggregateMonthlyKpi()           Tong hop KPI thang cho tat ca phong vien trong mot ky (Automated Aggregation Job)
 *  180  finalizeMonthlyKpi()            Chot ky KPI thang va luu snapshot bat bien (UC-66, BR-KPI-06)
 *  279  getDepartmentDashboard()        Lay du lieu tong the cho bang theo doi KPI toan ban (UC-61: Department KPI Board)
 *  384  getReporterKpiDetail()          Lay chi tiet KPI cua mot phong vien trong ky (UC-62, UC-60)
 *  ---- Ham phu (private, chi dung trong file) ----
 *   93  aggregateSnapshots()            Phan tinh va luu snapshot DRAFT, tra entity de finalizeMonthlyKpi chot tiep (UC-66)
 *  530  isReporter()                    Nguoi dung co vai tro Phong vien khong (roles.code = REPORTER)
 *  534  validatePeriod()                kiem tra period
 */
@Service
@RequiredArgsConstructor
public class KpiAggregationService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private final KpiSnapshotRepository kpiSnapshotRepository;
    private final KpiTargetRepository kpiTargetRepository;
    private final ArticleRepository articleRepository;
    private final ArticleAuthorRepository articleAuthorRepository;
    private final ReaderInteractionRepository readerInteractionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final AppealEffectiveScoreService appealEffectiveScoreService;  // BF-03: diem hieu luc cua bai

    // Dung may tinh toan thuan KpiCalculator (AGENTS.md muc 3.1)
    private final KpiCalculator kpiCalculator = new KpiCalculator();

    /**
     * Tong hop KPI thang cho tat ca phong vien trong mot ky (Automated Aggregation Job).
     * Luu hoac cap nhat ban ghi DRAFT vao kpi_snapshots.
     * Neu ban ghi da FINALIZED thi giu nguyen (BR-KPI-06).
     */
    @Transactional
    public List<KpiSnapshotResponse> aggregateMonthlyKpi(Short year, Byte month) {
        // Doi entity sang DTO o day, controller khong cam entity (AGENTS.md muc 3.2)
        List<KpiSnapshotResponse> result = new ArrayList<>();
        for (KpiSnapshot saved : aggregateSnapshots(year, month)) {
            result.add(KpiSnapshotResponse.from(saved));
        }
        return result;
    }

    // Phan tinh va luu snapshot DRAFT, tra entity de finalizeMonthlyKpi chot tiep (UC-66)
    private List<KpiSnapshot> aggregateSnapshots(Short year, Byte month) {
        validatePeriod(year, month);

        // 1. Lay tat ca phong vien co chi tieu KPI trong ky hoac tat ca users co role REPORTER
        List<KpiTarget> targets = kpiTargetRepository.findByTargetYearAndTargetMonth(year, month);
        Map<Integer, KpiTarget> targetMap = new HashMap<>();
        for (KpiTarget t : targets) {
            targetMap.put(t.getReporterId(), t);
        }

        // Lay tat ca users
        List<User> allUsers = userRepository.findAll();
        List<User> reporters = new ArrayList<>();
        for (User u : allUsers) {
            if (isReporter(u)) {
                reporters.add(u);
            }
        }

        // 2. Gom nhom cac bai viet theo tung phong vien kem share_percent (BR-KPI-02)
        Map<Integer, List<KpiCalculator.ArticleKpiItem>> reporterArticlesMap = buildReporterArticlesMap(year, month);

        // 3. Tinh toan va luu/cap nhat snapshot
        List<KpiSnapshot> savedSnapshots = new ArrayList<>();
        for (User reporter : reporters) {
            Optional<KpiSnapshot> existingOpt = kpiSnapshotRepository
                    .findByReporterIdAndPeriodYearAndPeriodMonth(reporter.getId(), year, month);

            // Neu da FINALIZED -> bat bien theo BR-KPI-06, bo qua khong tinh lai
            if (existingOpt.isPresent() && existingOpt.get().getStatus() == KpiSnapshotStatus.FINALIZED) {
                savedSnapshots.add(existingOpt.get());
                continue;
            }

            List<KpiCalculator.ArticleKpiItem> items = reporterArticlesMap.getOrDefault(reporter.getId(), Collections.emptyList());
            BigDecimal convertedArticles = kpiCalculator.calculateConvertedArticles(items);
            BigDecimal avgScore = kpiCalculator.calculateAverageScore(items);

            KpiSnapshot snapshot = existingOpt.orElseGet(KpiSnapshot::new);
            snapshot.setReporterId(reporter.getId());
            snapshot.setPeriodYear(year);
            snapshot.setPeriodMonth(month);
            snapshot.setActualArticles(convertedArticles);
            snapshot.setActualAvgScore(avgScore);
            snapshot.setStatus(KpiSnapshotStatus.DRAFT);

            savedSnapshots.add(kpiSnapshotRepository.save(snapshot));
        }
        return savedSnapshots;
    }

    /**
     * Chot ky KPI thang va luu snapshot bat bien (UC-66, BR-KPI-06).
     * Dieu kien nghiep vu:
     * - Neu ky da co snapshot o trang thai FINALIZED -> chan khong cho chot lai (E1: Period Already Finalized).
     * - Tinh toan xep loai PASS / NOT_PASS theo chi tieu (BR-KPI-05).
     * - Chuyen trang thai snapshot sang FINALIZED, luu nguoi chot va thoi diem chot.
     * - Gui thong bao ket qua cho tung phong vien (UC-67).
     * - Ghi nhat ky he thong audit_logs.
     */
    @Transactional
    public KpiFinalizeResponse finalizeMonthlyKpi(Short year, Byte month, Integer finalizedBy, Map<Integer, String> reporterNotes) {
        validatePeriod(year, month);

        // 1. Kiem tra xem ky da tung duoc chot truoc do chua (E1)
        List<KpiSnapshot> existingSnapshots = kpiSnapshotRepository.findByPeriodYearAndPeriodMonth(year, month);
        for (KpiSnapshot s : existingSnapshots) {
            if (s.getStatus() == KpiSnapshotStatus.FINALIZED) {
                throw new BusinessException("Kỳ KPI Tháng " + month + "/" + year + " đã được khóa và chốt kết quả bất biến, không thể chốt lại.");
            }
        }

        // 2. Chay tong hop du lieu KPI moi nhat cho ky
        List<KpiSnapshot> draftSnapshots = aggregateSnapshots(year, month);
        if (draftSnapshots.isEmpty()) {
            throw new BusinessException("Không có dữ liệu KPI để chốt cho kỳ " + month + "/" + year + ".");
        }
        Integer auditEntityId = null;

        // 3. Lay chi tieu de xet PASS / NOT_PASS
        List<KpiTarget> targets = kpiTargetRepository.findByTargetYearAndTargetMonth(year, month);
        Map<Integer, KpiTarget> targetMap = new HashMap<>();
        for (KpiTarget t : targets) {
            targetMap.put(t.getReporterId(), t);
        }

        LocalDateTime now = LocalDateTime.now();
        List<KpiSnapshotResponse> responseSnapshots = new ArrayList<>();
        int passCount = 0;
        int notPassCount = 0;

        for (KpiSnapshot snapshot : draftSnapshots) {
            // Gan ghi chu danh gia tu Truong ban neu co
            if (reporterNotes != null && reporterNotes.containsKey(snapshot.getReporterId())) {
                snapshot.setNotes(reporterNotes.get(snapshot.getReporterId()));
            }

            // Chuyen trang thai sang FINALIZED bat bien (BR-KPI-06)
            snapshot.setStatus(KpiSnapshotStatus.FINALIZED);
            snapshot.setFinalizedBy(finalizedBy);
            snapshot.setFinalizedAt(now);

            KpiSnapshot saved = kpiSnapshotRepository.save(snapshot);
            if (saved == null || saved.getId() == null) {
                throw new BusinessException("Không thể ghi snapshot KPI vì dữ liệu snapshot chưa có ID hợp lệ.");
            }
            if (auditEntityId == null) {
                auditEntityId = saved.getId();
            }
            responseSnapshots.add(KpiSnapshotResponse.from(saved));

            // Kiem tra PASS / NOT_PASS theo BR-KPI-05
            KpiTarget target = targetMap.get(snapshot.getReporterId());
            int minPublished = target != null ? target.getMinPublished() : 0;
            BigDecimal minAvgScore = target != null ? target.getMinAvgScore() : BigDecimal.ZERO;
            boolean isPass = kpiCalculator.evaluatePassStatus(
                    snapshot.getActualArticles(), snapshot.getActualAvgScore(), minPublished, minAvgScore
            );

            if (isPass) {
                passCount++;
            } else {
                notPassCount++;
            }

            // 4. Gui thong bao cho phong vien (UC-66 buoc 4, UC-67)
            String resultText = isPass ? "ĐẠT (PASS)" : "CHƯA ĐẠT (NOT_PASS)";
            String msg = String.format("Kết quả KPI Tháng %d/%d của bạn đã được chốt chính thức: %s. Số bài quy đổi: %d, Điểm TB: %s.",
                    month, year, resultText, snapshot.getActualArticles(),
                    snapshot.getActualAvgScore() != null ? snapshot.getActualAvgScore() : "—");
            notificationService.notifyUser(snapshot.getReporterId(), null, msg);
        }

        // 5. Ghi nhat ky kiem toan AuditLog
        auditService.log(
                finalizedBy,
                "FINALIZE_KPI",
                "kpi_snapshots",
                auditEntityId,
                "DRAFT",
                String.format("FINALIZED kỳ %d/%d: %d phóng viên (Đạt: %d, Chưa đạt: %d)", month, year, draftSnapshots.size(), passCount, notPassCount)
        );

        KpiFinalizeResponse response = new KpiFinalizeResponse();
        response.setYear(year);
        response.setMonth(month);
        response.setTotalReporters(draftSnapshots.size());
        response.setPassCount(passCount);
        response.setNotPassCount(notPassCount);
        response.setFinalizedBy(finalizedBy);
        response.setFinalizedAt(now);
        response.setSnapshots(responseSnapshots);

        return response;
    }

    /**
     * Lay du lieu tong the cho bang theo doi KPI toan ban (UC-61: Department KPI Board).
     */
    @Transactional(readOnly = true)
    public DepartmentKpiDashboardResponse getDepartmentDashboard(Short year, Byte month) {
        validatePeriod(year, month);

        // Lay tat ca phong vien co tai khoan
        List<User> allUsers = userRepository.findAll();
        Map<Integer, User> reporterUserMap = new HashMap<>();
        for (User u : allUsers) {
            if (isReporter(u)) {
                reporterUserMap.put(u.getId(), u);
            }
        }

        // Lay chi tieu thang
        List<KpiTarget> targets = kpiTargetRepository.findByTargetYearAndTargetMonth(year, month);
        Map<Integer, KpiTarget> targetMap = new HashMap<>();
        for (KpiTarget t : targets) {
            targetMap.put(t.getReporterId(), t);
        }

        // Lay snapshots da luu
        List<KpiSnapshot> snapshots = kpiSnapshotRepository.findByPeriodYearAndPeriodMonth(year, month);
        Map<Integer, KpiSnapshot> snapshotMap = new HashMap<>();
        for (KpiSnapshot s : snapshots) {
            snapshotMap.put(s.getReporterId(), s);
        }

        DepartmentKpiDashboardResponse response = new DepartmentKpiDashboardResponse();
        response.setYear(year);
        response.setMonth(month);

        Map<Integer, List<KpiCalculator.ArticleKpiItem>> reporterArticlesMap = buildReporterArticlesMap(year, month);

        List<DepartmentKpiItemResponse> items = new ArrayList<>();
        int passCount = 0;
        int notPassCount = 0;
        boolean allFinalized = !reporterUserMap.isEmpty();

        for (Map.Entry<Integer, User> entry : reporterUserMap.entrySet()) {
            Integer reporterId = entry.getKey();
            User reporter = entry.getValue();

            DepartmentKpiItemResponse item = new DepartmentKpiItemResponse();
            item.setReporterId(reporterId);
            item.setReporterName(reporter.getFullName() != null ? reporter.getFullName() : reporter.getUsername());
            item.setReporterEmail(reporter.getEmail());

            // Thiet lap chi tieu
            KpiTarget target = targetMap.get(reporterId);
            int minPublished = target != null ? target.getMinPublished() : 0;
            BigDecimal minAvgScore = target != null ? target.getMinAvgScore() : BigDecimal.ZERO;
            item.setMinPublished(minPublished);
            item.setMinAvgScore(minAvgScore);

            // Lay danh sach bai viet thuc te quy doi exactActualArticles (BR-KPI-02)
            List<KpiCalculator.ArticleKpiItem> kpiItems = reporterArticlesMap.getOrDefault(reporterId, Collections.emptyList());

            // Thiet lap thuc te: FINALIZED thi giu nguyen so da chot (BR-KPI-06); DRAFT thi tinh dong theo bai viet
            KpiSnapshot snapshot = snapshotMap.get(reporterId);
            KpiSnapshotStatus status = snapshot != null ? snapshot.getStatus() : KpiSnapshotStatus.DRAFT;
            BigDecimal actualArticles;
            BigDecimal actualAvgScore;

            if (snapshot != null && status == KpiSnapshotStatus.FINALIZED) {
                actualArticles = snapshot.getActualArticles();
                actualAvgScore = snapshot.getActualAvgScore();
            } else {
                actualArticles = kpiCalculator.calculateConvertedArticles(kpiItems);
                actualAvgScore = (snapshot != null && snapshot.getActualAvgScore() != null)
                        ? snapshot.getActualAvgScore()
                        : kpiCalculator.calculateAverageScore(kpiItems);
                allFinalized = false;
            }

            item.setActualArticles(actualArticles);
            item.setExactActualArticles(actualArticles);
            item.setActualAvgScore(actualAvgScore);
            item.setSnapshotStatus(status);
            item.setNotes(snapshot != null ? snapshot.getNotes() : null);

            // Tinh tien do bai viet % theo actualArticles
            if (minPublished > 0 && actualArticles != null) {
                BigDecimal progress = actualArticles
                        .multiply(ONE_HUNDRED)
                        .divide(BigDecimal.valueOf(minPublished), 1, RoundingMode.HALF_UP);
                item.setArticleProgressPercent(progress);
            } else {
                item.setArticleProgressPercent(BigDecimal.ZERO);
            }

            // Xet trang thai PASS / NOT_PASS (BR-KPI-05)
            boolean isPass = kpiCalculator.evaluatePassStatus(actualArticles, actualAvgScore, minPublished, minAvgScore);
            item.setProjectedStatus(isPass ? "PASS" : "NOT_PASS");

            if (isPass) {
                passCount++;
            } else {
                notPassCount++;
            }

            items.add(item);
        }

        response.setTotalReporters(items.size());
        response.setPassCount(passCount);
        response.setNotPassCount(notPassCount);
        response.setFinalized(allFinalized);
        response.setItems(items);

        return response;
    }

    /**
     * Lay chi tiet KPI cua mot phong vien trong ky (UC-62, UC-60).
     * Bao gom danh sach bai viet da xuat ban, ty le dong tac gia quy doi (0.6 / 0.4),
     * diem chat luong va luot xem tich luy tu doc gia.
     */
    @Transactional(readOnly = true)
    public ReporterKpiDetailResponse getReporterKpiDetail(Integer reporterId, Short year, Byte month,
                                                          Integer currentUserId) {
        validatePeriod(year, month);

        // Phong vien chi xem duoc KPI cua chinh minh (UC-60); Truong ban xem duoc moi phong vien (UC-62)
        User viewer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng với ID: " + currentUserId));
        if (isReporter(viewer) && !viewer.getId().equals(reporterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn chỉ xem được KPI của chính mình.");
        }

        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phóng viên với ID: " + reporterId));

        // 1. Lay chi tieu giao trong ky (kpi_targets)
        Optional<KpiTarget> targetOpt = kpiTargetRepository.findByReporterIdAndTargetYearAndTargetMonth(reporterId, year, month);
        int minPublished = targetOpt.map(KpiTarget::getMinPublished).orElse(0);
        BigDecimal minAvgScore = targetOpt.map(KpiTarget::getMinAvgScore).orElse(BigDecimal.ZERO);

        // 2. Lay snapshot (neu co)
        Optional<KpiSnapshot> snapshotOpt = kpiSnapshotRepository.findByReporterIdAndPeriodYearAndPeriodMonth(reporterId, year, month);

        // 3. Lay danh sach bai viet PUBLISHED trong thang va nam
        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);

        List<Article> allArticles = articleRepository.findAll();
        List<Article> publishedArticlesInPeriod = new ArrayList<>();
        List<Integer> publishedArticleIds = new ArrayList<>();
        for (Article a : allArticles) {
            if (a.getStatus() == ArticleStatus.PUBLISHED && a.getPublishedAt() != null) {
                if (!a.getPublishedAt().isBefore(startOfMonth) && !a.getPublishedAt().isAfter(endOfMonth)) {
                    publishedArticlesInPeriod.add(a);
                    publishedArticleIds.add(a.getId());
                }
            }
        }

        // 4. Loc cac bai ma phong vien la tac gia hoac dong tac gia
        List<ArticleAuthor> authorRecords = publishedArticleIds.isEmpty()
                ? Collections.emptyList()
                : articleAuthorRepository.findByUserIdAndArticleIdIn(reporterId, publishedArticleIds);

        Map<Integer, ArticleAuthor> authorMap = new HashMap<>();
        for (ArticleAuthor aa : authorRecords) {
            authorMap.put(aa.getArticleId(), aa);
        }

        // 5. Lay tong luot xem tich luy tu reader_interactions theo bai viet
        Map<Integer, Integer> pageViewsMap = new HashMap<>();
        if (!publishedArticleIds.isEmpty()) {
            List<Object[]> viewResults = readerInteractionRepository.sumPageViewsByArticleIds(publishedArticleIds);
            for (Object[] row : viewResults) {
                Integer artId = (Integer) row[0];
                Number sumViews = (Number) row[1];
                pageViewsMap.put(artId, sumViews != null ? sumViews.intValue() : 0);
            }
        }

        // 6. Tao danh sach bai viet chi tiet cua phong vien kem ty le dong tac gia
        List<ReporterKpiArticleItemResponse> articleResponses = new ArrayList<>();
        List<KpiCalculator.ArticleKpiItem> kpiItems = new ArrayList<>();
        int totalViews = 0;

        for (Article a : publishedArticlesInPeriod) {
            ArticleAuthor author = authorMap.get(a.getId());
            if (author != null) {
                BigDecimal sharePercent = author.getSharePercent() != null ? author.getSharePercent() : ONE_HUNDRED;
                BigDecimal converted = sharePercent.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
                Integer views = pageViewsMap.getOrDefault(a.getId(), 0);
                totalViews += views;

                // Diem hieu luc cua tung bai (BR-QC-10); bai chua cong bo diem thi null
                BigDecimal score = appealEffectiveScoreService.getEffectiveScore(a.getId());

                ReporterKpiArticleItemResponse item = new ReporterKpiArticleItemResponse();
                item.setArticleId(a.getId());
                item.setTitle(a.getTitle());
                item.setCategoryId(a.getCategoryId());
                item.setNature(a.getDeclaredNature() != null ? a.getDeclaredNature().name() : "ORIGINAL");
                item.setPublishedAt(a.getPublishedAt());
                item.setSharePercent(sharePercent);
                item.setConvertedArticles(converted);
                item.setEffectiveScore(score);
                item.setPageViews(views);

                articleResponses.add(item);
                kpiItems.add(new KpiCalculator.ArticleKpiItem(a.getId(), sharePercent, score));
            }
        }

        // 7. Tinh toan tien do va trang thai
        BigDecimal actualArticles;
        BigDecimal exactActualArticles;
        BigDecimal actualAvgScore;
        KpiSnapshotStatus snapshotStatus;
        String notes = null;

        // Snapshot FINALIZED la bat bien (BR-KPI-06) nen hien dung so da chot.
        // Snapshot DRAFT chi la so tam, nen tinh truc tiep tu bai viet de luon khop diem moi nhat.
        if (snapshotOpt.isPresent() && snapshotOpt.get().getStatus() == KpiSnapshotStatus.FINALIZED) {
            KpiSnapshot snap = snapshotOpt.get();
            actualArticles = snap.getActualArticles();
            exactActualArticles = snap.getActualArticles();
            actualAvgScore = snap.getActualAvgScore();
            snapshotStatus = snap.getStatus();
            notes = snap.getNotes();
        } else {
            actualArticles = kpiCalculator.calculateConvertedArticles(kpiItems);
            exactActualArticles = actualArticles;
            actualAvgScore = kpiCalculator.calculateAverageScore(kpiItems);
            snapshotStatus = KpiSnapshotStatus.DRAFT;
            notes = snapshotOpt.map(KpiSnapshot::getNotes).orElse(null);
        }

        BigDecimal progressPercent = BigDecimal.ZERO;
        if (minPublished > 0 && actualArticles != null) {
            progressPercent = actualArticles
                    .multiply(ONE_HUNDRED)
                    .divide(BigDecimal.valueOf(minPublished), 1, RoundingMode.HALF_UP);
        }

        boolean isPass = kpiCalculator.evaluatePassStatus(actualArticles, actualAvgScore, minPublished, minAvgScore);

        ReporterKpiDetailResponse detail = new ReporterKpiDetailResponse();
        detail.setReporterId(reporter.getId());
        detail.setReporterName(reporter.getFullName() != null ? reporter.getFullName() : reporter.getUsername());
        detail.setReporterEmail(reporter.getEmail());
        detail.setYear(year);
        detail.setMonth(month);
        detail.setMinPublished(minPublished);
        detail.setMinAvgScore(minAvgScore);
        detail.setActualArticles(actualArticles);
        detail.setExactActualArticles(exactActualArticles);
        detail.setActualAvgScore(actualAvgScore);
        detail.setTotalViews(totalViews);
        detail.setArticleProgressPercent(progressPercent);
        detail.setProjectedStatus(isPass ? "PASS" : "NOT_PASS");
        detail.setSnapshotStatus(snapshotStatus);
        detail.setNotes(notes);
        detail.setArticles(articleResponses);

        return detail;
    }

    // Nguoi dung co vai tro Phong vien khong (roles.code = REPORTER)
    private static boolean isReporter(User user) {
        return user.getRole() != null && user.getRole().getCode() == RoleCode.REPORTER;
    }

    /**
     * Gom nhom cac bai viet PUBLISHED trong ky theo phong vien kem diem hieu luc va ty le dong tac gia.
     * Dung chung cho ca aggregateSnapshots va getDepartmentDashboard.
     */
    private Map<Integer, List<KpiCalculator.ArticleKpiItem>> buildReporterArticlesMap(Short year, Byte month) {
        LocalDateTime startOfMonth = LocalDateTime.of(year, month, 1, 0, 0, 0);
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1).minusNanos(1);

        List<Article> allArticles = articleRepository.findAll();
        List<Article> publishedArticlesInPeriod = new ArrayList<>();
        for (Article a : allArticles) {
            if (a.getStatus() == ArticleStatus.PUBLISHED && a.getPublishedAt() != null) {
                if (!a.getPublishedAt().isBefore(startOfMonth) && !a.getPublishedAt().isAfter(endOfMonth)) {
                    publishedArticlesInPeriod.add(a);
                }
            }
        }

        Map<Integer, List<KpiCalculator.ArticleKpiItem>> reporterArticlesMap = new HashMap<>();
        for (Article article : publishedArticlesInPeriod) {
            BigDecimal effectiveScore = appealEffectiveScoreService.getEffectiveScore(article.getId());
            List<ArticleAuthor> authors = articleAuthorRepository.findByArticleIdOrderBySharePercentDesc(article.getId());
            for (ArticleAuthor author : authors) {
                Integer reporterId = author.getUserId();
                BigDecimal share = author.getSharePercent() != null ? author.getSharePercent() : ONE_HUNDRED;

                reporterArticlesMap.computeIfAbsent(reporterId, k -> new ArrayList<>())
                        .add(new KpiCalculator.ArticleKpiItem(article.getId(), share, effectiveScore));
            }
        }
        return reporterArticlesMap;
    }

    private void validatePeriod(Short year, Byte month) {
        if (year == null || year < 2000 || year > 2100) {
            throw new BusinessException("Năm không hợp lệ (phải từ 2000 đến 2100).");
        }
        if (month == null || month < 1 || month > 12) {
            throw new BusinessException("Tháng không hợp lệ (phải từ 1 đến 12).");
        }
    }
}
