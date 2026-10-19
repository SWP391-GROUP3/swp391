package com.swp391.kpi.service;

import com.swp391.kpi.dto.ArticlePublishedResponse;
import com.swp391.kpi.dto.RoyaltyPendingArticleResponse;
import com.swp391.kpi.dto.RoyaltyPeriodArticleResponse;
import com.swp391.kpi.dto.RoyaltyPeriodDetailResponse;
import com.swp391.kpi.dto.RoyaltyPeriodPersonResponse;
import com.swp391.kpi.dto.RoyaltyPeriodResponse;
import com.swp391.kpi.dto.UserAccessResponse;
import com.swp391.kpi.entity.RoyaltyGrading;
import com.swp391.kpi.entity.RoyaltyLine;
import com.swp391.kpi.entity.RoyaltyLineType;
import com.swp391.kpi.entity.RoyaltyPeriod;
import com.swp391.kpi.entity.RoyaltyPeriodStatus;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.RoyaltyGradingRepository;
import com.swp391.kpi.repository.RoyaltyLineRepository;
import com.swp391.kpi.repository.RoyaltyPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Ky nhuan but: danh sach ky (UC-49), bang ke 1 ky va Thu ky chot ky (UC-50).
 */
@Service
@RequiredArgsConstructor
public class RoyaltyPeriodService {

    private static final String ACCOUNTANT_ROLE = "ACCOUNTANT";  // roles.code cua Ke toan, nguoi nhan ky da chot

    private final RoyaltyPeriodRepository royaltyPeriodRepository;
    private final RoyaltyGradingRepository royaltyGradingRepository;
    private final RoyaltyLineRepository royaltyLineRepository;
    private final RoyaltyGradingService royaltyGradingService;      // BF-04: danh sach bai cho cham (UC-46)
    private final ArticlePublishedService articlePublishedService;  // service cua BF-01: tieu de bai
    private final UserReadService userReadService;                  // service luong admin: ho ten
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ------------------------------------------------------------------ UC-49

    // Cac ky moi nhat len dau, kem tong tien va so bai da cham.
    // Moi ky chay them 2 cau dem: so ky it (12 ky/nam) nen chap nhan duoc.
    @Transactional(readOnly = true)
    public List<RoyaltyPeriodResponse> getAllPeriods() {
        List<RoyaltyPeriodResponse> result = new ArrayList<>();
        for (RoyaltyPeriod period : royaltyPeriodRepository.findAllByOrderByPeriodYearDescPeriodMonthDesc()) {
            BigDecimal totalAmount = royaltyPeriodRepository.sumLineAmountByPeriodId(period.getId());
            Integer gradedArticleCount = royaltyPeriodRepository.countGradedArticlesByPeriodId(period.getId());
            result.add(RoyaltyPeriodResponse.from(period, totalAmount, gradedArticleCount));
        }
        return result;
    }

    // ------------------------------------------------------------------ UC-50

    // UC-50 buoc 1: bang ke ky = cac bai da cham trong ky + tong tien tung nguoi.
    // Thu ky xem truoc khi chot, Ke toan xem khi kiem tra (UC-51).
    @Transactional(readOnly = true)
    public RoyaltyPeriodDetailResponse getPeriodDetail(Integer periodId) {
        RoyaltyPeriod period = findPeriod(periodId);
        List<RoyaltyGrading> gradings = royaltyGradingRepository.findByPeriodIdOrderByGradedAtAsc(periodId);
        List<RoyaltyLine> lines = royaltyLineRepository.findByPeriodId(periodId);

        RoyaltyPeriodDetailResponse response = RoyaltyPeriodDetailResponse.from(period);
        response.setArticles(buildArticleRows(gradings, lines));
        response.setPeople(buildPersonRows(lines));
        response.setTotalAmount(sumAmount(lines));
        response.setGradedArticleCount(countArticles(gradings));
        return response;
    }

    // UC-50 buoc 2-3: Thu ky chot ky OPEN -> CLOSED (BR-RY-07), bao Ke toan,
    // roi tu tao ky thang sau de cham tiep (nhom chot 14/10/2026).
    @Transactional
    public RoyaltyPeriodResponse closePeriod(Integer periodId, Integer currentUserId) {
        RoyaltyPeriod period = findPeriod(periodId);
        if (period.getStatus() != RoyaltyPeriodStatus.OPEN) {
            throw new BusinessException("Chỉ chốt được kỳ đang mở. Kỳ " + label(period)
                    + " đang ở trạng thái " + period.getStatus() + ".");
        }
        checkAllArticlesOfMonthGraded(period);   // UC-50 E1

        period.setStatus(RoyaltyPeriodStatus.CLOSED);
        RoyaltyPeriod saved = royaltyPeriodRepository.save(period);
        createNextPeriodIfMissing(saved);

        auditService.log(currentUserId, "CLOSE", "royalty_periods", saved.getId(),
                "{\"status\":\"OPEN\"}", "{\"status\":\"CLOSED\"}");
        notificationService.notifyRole(ACCOUNTANT_ROLE,
                "Kỳ nhuận bút " + label(saved) + " đã được Thư ký chốt, chờ Kế toán kiểm tra.");

        BigDecimal totalAmount = royaltyPeriodRepository.sumLineAmountByPeriodId(saved.getId());
        Integer gradedArticleCount = royaltyPeriodRepository.countGradedArticlesByPeriodId(saved.getId());
        return RoyaltyPeriodResponse.from(saved, totalAmount, gradedArticleCount);
    }

    // ------------------------------------------------------------- kiem tra

    // UC-50 E1 (nhom chot 18/10/2026): con bai da dang tu truoc toi het thang cua ky
    // ma chua cham thi chua cho chot. Bai dang sang thang sau KHONG chan: cham xong
    // se vao ky dang mo luc do (BR-RY-05).
    private void checkAllArticlesOfMonthGraded(RoyaltyPeriod period) {
        LocalDate lastDayOfMonth = YearMonth.of(period.getPeriodYear(), period.getPeriodMonth()).atEndOfMonth();
        int ungradedCount = 0;
        for (RoyaltyPendingArticleResponse article : royaltyGradingService.getPendingArticles()) {
            if (!article.getPublishedAt().toLocalDate().isAfter(lastDayOfMonth)) {
                ungradedCount++;
            }
        }
        if (ungradedCount > 0) {
            throw new BusinessException("Còn " + ungradedCount + " bài đăng đến hết tháng " + label(period)
                    + " chưa chấm nhuận bút. Vui lòng chấm xong trước khi chốt kỳ.");
        }
    }

    // Chot ky thang N thi tu tao ky thang N+1 (OPEN) neu chua co, de Thu ky cham tiep
    // duoc (UC-47 E1 can ky OPEN). Thang 12 -> thang 1 nam sau (YearMonth tu tinh).
    private void createNextPeriodIfMissing(RoyaltyPeriod period) {
        YearMonth next = YearMonth.of(period.getPeriodYear(), period.getPeriodMonth()).plusMonths(1);
        Short nextYear = (short) next.getYear();
        Byte nextMonth = (byte) next.getMonthValue();
        if (royaltyPeriodRepository.existsByPeriodYearAndPeriodMonth(nextYear, nextMonth)) {
            return;
        }
        RoyaltyPeriod nextPeriod = new RoyaltyPeriod();
        nextPeriod.setPeriodYear(nextYear);
        nextPeriod.setPeriodMonth(nextMonth);
        nextPeriod.setStatus(RoyaltyPeriodStatus.OPEN);
        royaltyPeriodRepository.save(nextPeriod);
    }

    // ------------------------------------------------------------- bang ke

    // Moi phieu cham 1 dong: tien bai + tong tien anh cua phieu (cong tu dong MEDIA)
    private List<RoyaltyPeriodArticleResponse> buildArticleRows(List<RoyaltyGrading> gradings,
                                                                List<RoyaltyLine> lines) {
        Map<Integer, BigDecimal> mediaAmountByGrading = new HashMap<>();
        for (RoyaltyLine line : lines) {
            if (line.getLineType() == RoyaltyLineType.MEDIA) {
                Integer gradingId = line.getGrading().getId();
                BigDecimal current = mediaAmountByGrading.getOrDefault(gradingId, BigDecimal.ZERO);
                mediaAmountByGrading.put(gradingId, current.add(line.getAmount()));
            }
        }

        Map<Integer, String> titles = loadArticleTitles();
        List<RoyaltyPeriodArticleResponse> result = new ArrayList<>();
        for (RoyaltyGrading grading : gradings) {
            BigDecimal mediaAmount = mediaAmountByGrading.getOrDefault(grading.getId(), BigDecimal.ZERO);
            result.add(RoyaltyPeriodArticleResponse.from(grading, titles.get(grading.getArticleId()), mediaAmount));
        }
        return result;
    }

    // Moi nguoi 1 dong: cong cac dong tien cua nguoi do theo loai. TreeMap -> xep theo id nguoi.
    private List<RoyaltyPeriodPersonResponse> buildPersonRows(List<RoyaltyLine> lines) {
        Map<Integer, String> names = loadUserNames();
        Map<Integer, RoyaltyPeriodPersonResponse> people = new TreeMap<>();
        for (RoyaltyLine line : lines) {
            RoyaltyPeriodPersonResponse person = people.get(line.getUserId());
            if (person == null) {
                person = new RoyaltyPeriodPersonResponse();
                person.setUserId(line.getUserId());
                person.setFullName(names.get(line.getUserId()));
                people.put(line.getUserId(), person);
            }
            addLineAmount(person, line);
        }
        return new ArrayList<>(people.values());
    }

    private static void addLineAmount(RoyaltyPeriodPersonResponse person, RoyaltyLine line) {
        BigDecimal amount = line.getAmount();
        switch (line.getLineType()) {
            case ARTICLE -> person.setArticleAmount(person.getArticleAmount().add(amount));
            case MEDIA -> person.setMediaAmount(person.getMediaAmount().add(amount));
            case ADJUSTMENT -> person.setAdjustmentAmount(person.getAdjustmentAmount().add(amount));
        }
        person.setTotalAmount(person.getTotalAmount().add(amount));
    }

    // Tieu de bai, lay qua service BF-01 (khong query bang articles)
    private Map<Integer, String> loadArticleTitles() {
        Map<Integer, String> titles = new HashMap<>();
        for (ArticlePublishedResponse article : articlePublishedService.getPublishedArticles()) {
            titles.put(article.getId(), article.getTitle());
        }
        return titles;
    }

    // Ho ten nguoi nhan tien, lay qua service luong admin (khong query bang users)
    private Map<Integer, String> loadUserNames() {
        Map<Integer, String> names = new HashMap<>();
        for (UserAccessResponse user : userReadService.getUsers()) {
            names.put(user.id(), user.fullName());
        }
        return names;
    }

    private static BigDecimal sumAmount(List<RoyaltyLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        for (RoyaltyLine line : lines) {
            total = total.add(line.getAmount());
        }
        return total;
    }

    // 1 bai cham lai nhieu phien ban van tinh 1 bai
    private static int countArticles(List<RoyaltyGrading> gradings) {
        Set<Integer> articleIds = new HashSet<>();
        for (RoyaltyGrading grading : gradings) {
            articleIds.add(grading.getArticleId());
        }
        return articleIds.size();
    }

    private RoyaltyPeriod findPeriod(Integer periodId) {
        return royaltyPeriodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ nhuận bút có ID: " + periodId));
    }

    // Vd "9/2026"
    private static String label(RoyaltyPeriod period) {
        return period.getPeriodMonth() + "/" + period.getPeriodYear();
    }
}
