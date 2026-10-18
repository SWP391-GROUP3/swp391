package com.swp391.kpi.service;

import com.swp391.kpi.dto.DepartmentKpiDashboardResponse;
import com.swp391.kpi.dto.DepartmentKpiItemResponse;
import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleAuthor;
import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.entity.KpiSnapshot;
import com.swp391.kpi.entity.KpiSnapshotStatus;
import com.swp391.kpi.entity.KpiTarget;
import com.swp391.kpi.entity.User;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.repository.ArticleAuthorRepository;
import com.swp391.kpi.repository.ArticleRepository;
import com.swp391.kpi.repository.KpiSnapshotRepository;
import com.swp391.kpi.repository.KpiTargetRepository;
import com.swp391.kpi.repository.UserRepository;
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
 */
@Service
@RequiredArgsConstructor
public class KpiAggregationService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final KpiSnapshotRepository kpiSnapshotRepository;
    private final KpiTargetRepository kpiTargetRepository;
    private final ArticleRepository articleRepository;
    private final ArticleAuthorRepository articleAuthorRepository;
    private final UserRepository userRepository;

    // Dung may tinh toan thuan KpiCalculator (AGENTS.md muc 3.1)
    private final KpiCalculator kpiCalculator = new KpiCalculator();

    /**
     * Tong hop KPI thang cho tat ca phong vien trong mot ky (Automated Aggregation Job).
     * Luu hoac cap nhat ban ghi DRAFT vao kpi_snapshots.
     * Neu ban ghi da FINALIZED thi giu nguyen (BR-KPI-06).
     */
    @Transactional
    public List<KpiSnapshot> aggregateMonthlyKpi(Short year, Byte month) {
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
            if (u.getRole() != null && "REPORTER".equals(u.getRole().getCode())) {
                reporters.add(u);
            }
        }

        // 2. Lay danh sach tat ca bai viet PUBLISHED trong thang va nam tuong ung
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

        // 3. Gom nhom cac bai viet theo tung phong vien kem share_percent (BR-KPI-02)
        Map<Integer, List<KpiCalculator.ArticleKpiItem>> reporterArticlesMap = new HashMap<>();
        for (Article article : publishedArticlesInPeriod) {
            List<ArticleAuthor> authors = articleAuthorRepository.findByArticleIdOrderBySharePercentDesc(article.getId());
            for (ArticleAuthor author : authors) {
                Integer reporterId = author.getUserId();
                BigDecimal share = author.getSharePercent() != null ? author.getSharePercent() : ONE_HUNDRED;
                
                // Diem chat luong bai viet tam thoi mac dinh hoac lay tu he thong
                BigDecimal effectiveScore = null;

                reporterArticlesMap.computeIfAbsent(reporterId, k -> new ArrayList<>())
                        .add(new KpiCalculator.ArticleKpiItem(article.getId(), share, effectiveScore));
            }
        }

        // 4. Tinh toan va luu/cap nhat snapshot
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
            int convertedArticles = kpiCalculator.calculateConvertedArticles(items);
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
     * Lay du lieu tong the cho bang theo doi KPI toan ban (UC-61: Department KPI Board).
     */
    @Transactional(readOnly = true)
    public DepartmentKpiDashboardResponse getDepartmentDashboard(Short year, Byte month) {
        validatePeriod(year, month);

        // Lay tat ca phong vien co tai khoan
        List<User> allUsers = userRepository.findAll();
        Map<Integer, User> reporterUserMap = new HashMap<>();
        for (User u : allUsers) {
            if (u.getRole() != null && "REPORTER".equals(u.getRole().getCode())) {
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

            // Thiet lap thuc te tu snapshot (neu co)
            KpiSnapshot snapshot = snapshotMap.get(reporterId);
            int actualArticles = snapshot != null ? snapshot.getActualArticles() : 0;
            BigDecimal actualAvgScore = snapshot != null ? snapshot.getActualAvgScore() : null;
            KpiSnapshotStatus status = snapshot != null ? snapshot.getStatus() : KpiSnapshotStatus.DRAFT;

            if (status != KpiSnapshotStatus.FINALIZED) {
                allFinalized = false;
            }

            item.setActualArticles(actualArticles);
            item.setExactActualArticles(BigDecimal.valueOf(actualArticles).setScale(2, RoundingMode.HALF_UP));
            item.setActualAvgScore(actualAvgScore);
            item.setSnapshotStatus(status);
            item.setNotes(snapshot != null ? snapshot.getNotes() : null);

            // Tinh tien do bai viet %
            if (minPublished > 0) {
                BigDecimal progress = BigDecimal.valueOf(actualArticles)
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

    private void validatePeriod(Short year, Byte month) {
        if (year == null || year < 2000 || year > 2100) {
            throw new BusinessException("Năm không hợp lệ (phải từ 2000 đến 2100).");
        }
        if (month == null || month < 1 || month > 12) {
            throw new BusinessException("Tháng không hợp lệ (phải từ 1 đến 12).");
        }
    }
}
