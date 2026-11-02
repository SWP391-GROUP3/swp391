package com.swp391.kpi.service;

import com.swp391.kpi.dto.ArticlePublishedResponse;
import com.swp391.kpi.dto.RoyaltyPendingArticleResponse;
import com.swp391.kpi.dto.RoyaltyPeriodArticleResponse;
import com.swp391.kpi.dto.RoyaltyPeriodDetailResponse;
import com.swp391.kpi.dto.RoyaltyPeriodPersonResponse;
import com.swp391.kpi.dto.RoyaltyPeriodResponse;
import com.swp391.kpi.entity.RoyaltyGrading;
import com.swp391.kpi.entity.RoyaltyLine;
import com.swp391.kpi.entity.RoyaltyLineType;
import com.swp391.kpi.entity.RoyaltyPayment;
import com.swp391.kpi.entity.RoyaltyPaymentStatus;
import com.swp391.kpi.entity.RoyaltyPeriod;
import com.swp391.kpi.entity.RoyaltyPeriodStatus;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.repository.RoyaltyGradingRepository;
import com.swp391.kpi.repository.RoyaltyLineRepository;
import com.swp391.kpi.repository.RoyaltyPaymentRepository;
import com.swp391.kpi.repository.RoyaltyPeriodRepository;
import com.swp391.kpi.repository.RoyaltyPeriodSummaryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Ky nhuan but: danh sach ky (UC-49), chot ky (UC-50), Ke toan tra ky (UC-52) va khoa ky (UC-53).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   75  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   78  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   93  getAllPeriods()                 Cac ky moi nhat len dau, kem tong tien va so bai da cham (chi 2 cau SQL)
 *  112  getPeriodDetail()              UC-50 buoc 1: bang ke ky = cac bai da cham trong ky + tong tien tung nguoi
 *  129  closePeriod()                  UC-50 buoc 2-3: Thu ky chot ky OPEN -> CLOSED (BR-RY-07), bao Ke toan, roi tu tao ky thang sau de cham tiep (…
 *  151  returnPeriod()                 UC-52 / UC-53 A1: Ke toan tra ky CLOSED ve OPEN kem ghi chu bat buoc (BR-RY-07), bao Thu ky sua roi chot lai …
 *  173  lockPeriod()                    UC-53: Ke toan khoa ky CLOSED -> LOCKED (BR-RY-07)
 *  ---- Ham phu (private, chi dung trong file) ----
 *  203  loadDebtFromPreviousPeriod()    No chuyen tiep = debt_out tren phieu chi cua ky thang truoc (GBR-12, UC-53 A2)
 *  226  groupLinesByUser()              Gom dong tien theo nguoi nhan
 *  239  createPayment()                1 phieu chi UNPAID cho 1 nguoi; gross = tong dong tien, tien chi va no tinh bang RoyaltyPaymentCalculator (ph…
 *  263  checkAllArticlesOfMonthGraded()  UC-50 E1 (nhom chot 18/10/2026): con bai da dang tu truoc toi het thang cua ky ma chua cham thi chua cho chot
 *  279  createNextPeriodIfMissing()     Chot ky thang N thi tu tao ky thang N+1 (OPEN) neu chua co, de Thu ky cham tiep duoc (UC-47 E1 can ky OPEN)
 *  297  keepLatestVersions()            Cham lai trong ky OPEN tao phien ban moi cung ky (dong tien ban cu da xoa), nen bang ke chi giu phien ban moi…
 *  309  buildArticleRows()              Moi phieu cham 1 dong: tien bai + tong tien anh cua phieu (cong tu dong MEDIA)
 *  330  buildPersonRows()               Moi nguoi 1 dong: cong cac dong tien cua nguoi do theo loai
 *  346  addLineAmount()                 them line amount
 *  357  loadArticleTitles()             Tieu de bai, lay qua service BF-01 (khong query bang articles)
 *  365  sumAmount()                     sum amount
 *  374  countArticles()                 1 bai cham lai nhieu phien ban van tinh 1 bai
 *  382  findPeriod()                    tim period
 *  388  requireStatus()                 BR-RY-07: thao tac chi lam duoc o 1 trang thai, sai thi bao loi kem trang thai hien tai
 *  396  toResponse()                    ky vua doi trang thai -> response kem tong tien va so bai (chot / tra / khoa ky dung chung)
 *  403  label()                         Vd "9/2026"
 */
@Service
@RequiredArgsConstructor
public class RoyaltyPeriodService {

    private static final String ACCOUNTANT_ROLE = "ACCOUNTANT";  // roles.code cua Ke toan, nguoi nhan ky da chot
    private static final String SECRETARY_ROLE = "EDITORIAL_SECRETARY";  // roles.code cua Thu ky, nguoi nhan ky bi tra

    private final RoyaltyPeriodRepository royaltyPeriodRepository;
    private final RoyaltyGradingRepository royaltyGradingRepository;
    private final RoyaltyLineRepository royaltyLineRepository;
    private final RoyaltyPaymentRepository royaltyPaymentRepository;
    private final RoyaltyGradingService royaltyGradingService;     // BF-04: danh sach bai cho cham (UC-46)
    private final ArticlePublishedService articlePublishedService;  // service cua BF-01: tieu de bai
    private final UserReadService userReadService;                  // service luong admin: ho ten
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ------------------------------------------------------------------ UC-49

    // Cac ky moi nhat len dau, kem tong tien va so bai da cham.
    // Chi 2 cau SQL du co bao nhieu ky: 1 cau lay ky, 1 cau lay tong tien + so bai cua moi ky.
    @Transactional(readOnly = true)
    public List<RoyaltyPeriodResponse> getAllPeriods() {
        Map<Integer, RoyaltyPeriodSummaryRow> summaryByPeriod = new HashMap<>();
        for (RoyaltyPeriodSummaryRow summary : royaltyPeriodRepository.findPeriodSummaries()) {
            summaryByPeriod.put(summary.getPeriodId(), summary);
        }

        List<RoyaltyPeriodResponse> result = new ArrayList<>();
        for (RoyaltyPeriod period : royaltyPeriodRepository.findAllByOrderByPeriodYearDescPeriodMonthDesc()) {
            RoyaltyPeriodSummaryRow summary = summaryByPeriod.get(period.getId());
            result.add(RoyaltyPeriodResponse.from(period, summary.getTotalAmount(), summary.getGradedArticleCount()));
        }
        return result;
    }

    // ------------------------------------------------------------------ UC-50

    // UC-50 buoc 1: bang ke ky = cac bai da cham trong ky + tong tien tung nguoi.
    // Thu ky xem truoc khi chot, Ke toan xem khi kiem tra (UC-51).
    @Transactional(readOnly = true)
    public RoyaltyPeriodDetailResponse getPeriodDetail(Integer periodId) {
        RoyaltyPeriod period = findPeriod(periodId);
        List<RoyaltyGrading> gradings = keepLatestVersions(
                royaltyGradingRepository.findByPeriodIdOrderByGradedAtAsc(periodId));
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
        requireStatus(period, RoyaltyPeriodStatus.OPEN, "Chỉ chốt được kỳ đang mở.");
        checkAllArticlesOfMonthGraded(period);   // UC-50 E1

        period.setStatus(RoyaltyPeriodStatus.CLOSED);
        RoyaltyPeriod saved = royaltyPeriodRepository.save(period);
        createNextPeriodIfMissing(saved);

        auditService.log(currentUserId, "CLOSE", "royalty_periods", saved.getId(),
                "{\"status\":\"OPEN\"}", "{\"status\":\"CLOSED\"}");
        notificationService.notifyRole(ACCOUNTANT_ROLE,
                "Kỳ nhuận bút " + label(saved) + " đã được Thư ký chốt, chờ Kế toán kiểm tra.");

        return toResponse(saved);
    }

    // ------------------------------------------------------------------ UC-52

    // UC-52 / UC-53 A1: Ke toan tra ky CLOSED ve OPEN kem ghi chu bat buoc (BR-RY-07),
    // bao Thu ky sua roi chot lai (UC-50 A1). Chi giu ghi chu cua lan tra gan nhat.
    @Transactional
    public RoyaltyPeriodResponse returnPeriod(Integer periodId, String note, Integer currentUserId) {
        RoyaltyPeriod period = findPeriod(periodId);
        requireStatus(period, RoyaltyPeriodStatus.CLOSED, "Chỉ trả lại được kỳ đã chốt.");

        period.setStatus(RoyaltyPeriodStatus.OPEN);
        period.setReturnedBy(currentUserId);
        period.setReturnNote(note.trim());
        RoyaltyPeriod saved = royaltyPeriodRepository.save(period);

        auditService.log(currentUserId, "RETURN", "royalty_periods", saved.getId(),
                "{\"status\":\"CLOSED\"}", "{\"status\":\"OPEN\"}");
        notificationService.notifyRole(SECRETARY_ROLE,
                "Kế toán đã trả lại kỳ nhuận bút " + label(saved) + ": " + saved.getReturnNote());

        return toResponse(saved);
    }

    // ------------------------------------------------------------------ UC-53

    // UC-53: Ke toan khoa ky CLOSED -> LOCKED (BR-RY-07). Moi nguoi 1 phieu chi UNPAID,
    // cac dong tien cua nguoi do gan vao phieu. Tu day so lieu cua ky la bat bien (BR-RY-06).
    @Transactional
    public RoyaltyPeriodResponse lockPeriod(Integer periodId, Integer currentUserId) {
        RoyaltyPeriod period = findPeriod(periodId);
        requireStatus(period, RoyaltyPeriodStatus.CLOSED, "Chỉ khoá được kỳ đã chốt.");
        Map<Integer, BigDecimal> debtByUser = loadDebtFromPreviousPeriod(period);

        Map<Integer, List<RoyaltyLine>> linesByUser = groupLinesByUser(royaltyLineRepository.findByPeriodId(periodId));
        // Nhom chot 27/10/2026: nguoi con no ky truoc ma ky nay khong co dong tien nao
        // van co phieu 0d, de no luon doc duoc tu phieu cua ky ngay truoc.
        for (Integer userId : debtByUser.keySet()) {
            if (!linesByUser.containsKey(userId)) {
                linesByUser.put(userId, new ArrayList<>());
            }
        }
        for (Map.Entry<Integer, List<RoyaltyLine>> entry : linesByUser.entrySet()) {
            BigDecimal debtIn = debtByUser.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            createPayment(period, entry.getKey(), entry.getValue(), debtIn);
        }

        period.setStatus(RoyaltyPeriodStatus.LOCKED);
        period.setLockedBy(currentUserId);
        period.setLockedAt(LocalDateTime.now());
        RoyaltyPeriod saved = royaltyPeriodRepository.save(period);
        auditService.log(currentUserId, "LOCK", "royalty_periods", saved.getId(),
                "{\"status\":\"CLOSED\"}", "{\"status\":\"LOCKED\"}");

        return toResponse(saved);
    }

    // No chuyen tiep = debt_out tren phieu chi cua ky thang truoc (GBR-12, UC-53 A2).
    // Ky truoc chua khoa thi chua biet ai con no bao nhieu -> chua cho khoa ky nay.
    private Map<Integer, BigDecimal> loadDebtFromPreviousPeriod(RoyaltyPeriod period) {
        YearMonth previousMonth = YearMonth.of(period.getPeriodYear(), period.getPeriodMonth()).minusMonths(1);
        Optional<RoyaltyPeriod> previous = royaltyPeriodRepository.findByPeriodYearAndPeriodMonth(
                (short) previousMonth.getYear(), (byte) previousMonth.getMonthValue());

        Map<Integer, BigDecimal> debtByUser = new HashMap<>();
        if (previous.isEmpty()) {
            return debtByUser;   // ky dau tien, chua ai no
        }
        RoyaltyPeriodStatus previousStatus = previous.get().getStatus();
        if (previousStatus == RoyaltyPeriodStatus.OPEN || previousStatus == RoyaltyPeriodStatus.CLOSED) {
            throw new BusinessException("Kỳ " + label(previous.get()) + " chưa khoá. Vui lòng khoá kỳ "
                    + label(previous.get()) + " trước rồi mới khoá kỳ " + label(period) + ".");
        }
        for (RoyaltyPayment payment : royaltyPaymentRepository.findByPeriodId(previous.get().getId())) {
            if (payment.getDebtOut().signum() > 0) {
                debtByUser.put(payment.getUserId(), payment.getDebtOut());
            }
        }
        return debtByUser;
    }

    // Gom dong tien theo nguoi nhan. TreeMap -> tao phieu theo thu tu id nguoi.
    private static Map<Integer, List<RoyaltyLine>> groupLinesByUser(List<RoyaltyLine> lines) {
        Map<Integer, List<RoyaltyLine>> linesByUser = new TreeMap<>();
        for (RoyaltyLine line : lines) {
            if (!linesByUser.containsKey(line.getUserId())) {
                linesByUser.put(line.getUserId(), new ArrayList<>());
            }
            linesByUser.get(line.getUserId()).add(line);
        }
        return linesByUser;
    }

    // 1 phieu chi UNPAID cho 1 nguoi; gross = tong dong tien, tien chi va no tinh bang
    // RoyaltyPaymentCalculator (phieu khong am, GBR-12). Sau do gan cac dong tien vao phieu.
    private void createPayment(RoyaltyPeriod period, Integer userId, List<RoyaltyLine> lines, BigDecimal debtIn) {
        BigDecimal grossAmount = sumAmount(lines);

        RoyaltyPayment payment = new RoyaltyPayment();
        payment.setPeriod(period);
        payment.setUserId(userId);
        payment.setGrossAmount(grossAmount);
        payment.setDebtIn(debtIn);
        payment.setPaidAmount(RoyaltyPaymentCalculator.paidAmount(grossAmount, debtIn));
        payment.setDebtOut(RoyaltyPaymentCalculator.debtOut(grossAmount, debtIn));
        payment.setStatus(RoyaltyPaymentStatus.UNPAID);   // paid_by, paid_at de NULL (CHECK ck_royalty_payments_status)
        RoyaltyPayment saved = royaltyPaymentRepository.save(payment);

        for (RoyaltyLine line : lines) {
            line.setPayment(saved);
        }
        royaltyLineRepository.saveAll(lines);
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

    // Cham lai trong ky OPEN tao phien ban moi cung ky (dong tien ban cu da xoa), nen bang ke
    // chi giu phien ban moi nhat cua moi bai. LinkedHashMap giu thu tu cham nhu danh sach goc.
    private static List<RoyaltyGrading> keepLatestVersions(List<RoyaltyGrading> gradings) {
        Map<Integer, RoyaltyGrading> latestByArticle = new LinkedHashMap<>();
        for (RoyaltyGrading grading : gradings) {
            RoyaltyGrading current = latestByArticle.get(grading.getArticleId());
            if (current == null || grading.getVersionNo() > current.getVersionNo()) {
                latestByArticle.put(grading.getArticleId(), grading);
            }
        }
        return new ArrayList<>(latestByArticle.values());
    }

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
        Map<Integer, String> names = userReadService.getUserNames();   // ho ten nguoi nhan tien
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

    // BR-RY-07: moi thao tac chi lam duoc o 1 trang thai; sai thi bao kem trang thai hien tai
    private static void requireStatus(RoyaltyPeriod period, RoyaltyPeriodStatus expected, String message) {
        if (period.getStatus() != expected) {
            throw new BusinessException(message + " Kỳ " + label(period)
                    + " đang ở trạng thái " + period.getStatus() + ".");
        }
    }

    // Ky vua doi trang thai -> response kem tong tien va so bai (dung chung cho chot / tra / khoa ky)
    private RoyaltyPeriodResponse toResponse(RoyaltyPeriod period) {
        BigDecimal totalAmount = royaltyPeriodRepository.sumLineAmountByPeriodId(period.getId());
        Integer gradedArticleCount = royaltyPeriodRepository.countGradedArticlesByPeriodId(period.getId());
        return RoyaltyPeriodResponse.from(period, totalAmount, gradedArticleCount);
    }

    // Vd "9/2026"
    private static String label(RoyaltyPeriod period) {
        return period.getPeriodMonth() + "/" + period.getPeriodYear();
    }
}
