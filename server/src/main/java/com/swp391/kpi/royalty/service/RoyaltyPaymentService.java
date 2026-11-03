package com.swp391.kpi.royalty.service;

import com.swp391.kpi.admin.service.UserReadService;
import com.swp391.kpi.article.dto.ArticlePublishedResponse;
import com.swp391.kpi.article.service.ArticlePublishedService;
import com.swp391.kpi.common.service.AuditService;
import com.swp391.kpi.common.service.NotificationService;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.exception.NotFoundException;
import com.swp391.kpi.royalty.dto.RoyaltyMyLineResponse;
import com.swp391.kpi.royalty.dto.RoyaltyMyPeriodResponse;
import com.swp391.kpi.royalty.dto.RoyaltyPaymentResponse;
import com.swp391.kpi.royalty.entity.RoyaltyLine;
import com.swp391.kpi.royalty.entity.RoyaltyPayment;
import com.swp391.kpi.royalty.entity.RoyaltyPaymentStatus;
import com.swp391.kpi.royalty.entity.RoyaltyPeriod;
import com.swp391.kpi.royalty.entity.RoyaltyPeriodStatus;
import com.swp391.kpi.royalty.repository.RoyaltyLineRepository;
import com.swp391.kpi.royalty.repository.RoyaltyPaymentRepository;
import com.swp391.kpi.royalty.repository.RoyaltyPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phieu chi nhuan but: Ke toan ghi nhan da chi (UC-54), phong vien xem nhuan but cua minh (UC-57).
 * Phieu chi duoc tao luc Ke toan khoa ky (RoyaltyPeriodService.lockPeriod, UC-53).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   56  field                           repository / service duoc inject
 *  ---- Ham public (controller / luong khac goi) ----
 *   68  getPaymentsOfPeriod()           UC-54 buoc 1: cac phieu chi cua 1 ky cho Ke toan chon
 *   82  recordPayment()                 UC-54 buoc 2-3: ghi nhan da chi 1 phieu; het phieu UNPAID thi ky sang PAID
 *  111  getMyRoyalties()                UC-57: nhuan but cua nguoi dang dang nhap, gom theo ky, ky moi nhat len dau
 *  ---- Ham phu (private, chi dung trong file) ----
 *  148  markPeriodPaidIfAllPaid()       BR-RY-07: moi phieu cua ky da PAID -> ky PAID, bao nhung nguoi nhan tien
 *  166  newPeriodRow()                  1 dong ky rong cho man UC-57
 *  175  loadArticleTitles()             Tieu de bai, lay qua service BF-01 (khong query bang articles)
 *  184  findPeriod()                    tim period
 *  190  label()                         Vd "9/2026"
 */
@Service
@RequiredArgsConstructor
public class RoyaltyPaymentService {

    private final RoyaltyPaymentRepository royaltyPaymentRepository;
    private final RoyaltyPeriodRepository royaltyPeriodRepository;
    private final RoyaltyLineRepository royaltyLineRepository;
    private final ArticlePublishedService articlePublishedService;  // service cua BF-01: tieu de bai
    private final UserReadService userReadService;                  // ho ten nguoi nhan tien
    private final NotificationService notificationService;
    private final AuditService auditService;

    // ------------------------------------------------------------------ UC-54

    // UC-54 buoc 1: Ke toan mo ky da khoa, thay phieu chi cua tung nguoi (so tien, no, trang thai)
    @Transactional(readOnly = true)
    public List<RoyaltyPaymentResponse> getPaymentsOfPeriod(Integer periodId) {
        findPeriod(periodId);   // ky khong ton tai -> 404
        Map<Integer, String> names = userReadService.getUserNames();
        List<RoyaltyPaymentResponse> result = new ArrayList<>();
        for (RoyaltyPayment payment : royaltyPaymentRepository.findByPeriodIdOrderByUserIdAsc(periodId)) {
            result.add(RoyaltyPaymentResponse.from(payment, names.get(payment.getUserId())));
        }
        return result;
    }

    // UC-54 buoc 2-3: Ke toan xac nhan da chi 1 phieu. Phieu 0d (nguoi dang no) cung phai xac nhan
    // de ky co the sang PAID. Het phieu UNPAID thi ky tu sang PAID (BR-RY-07).
    @Transactional
    public RoyaltyPaymentResponse recordPayment(Integer paymentId, Integer currentUserId) {
        RoyaltyPayment payment = royaltyPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiếu chi có ID: " + paymentId));
        // Kiem phieu truoc: phieu cuoi chi xong thi ky da PAID, bam lai van phai bao dung "phieu da chi"
        if (payment.getStatus() == RoyaltyPaymentStatus.PAID) {
            throw new BusinessException("Phiếu chi này đã được chi, không chi lại được.");   // UC-54 E1
        }
        RoyaltyPeriod period = payment.getPeriod();
        if (period.getStatus() != RoyaltyPeriodStatus.LOCKED) {
            throw new BusinessException("Chỉ ghi nhận chi được khi kỳ đã khoá. Kỳ " + label(period)
                    + " đang ở trạng thái " + period.getStatus() + ".");
        }

        payment.setStatus(RoyaltyPaymentStatus.PAID);
        payment.setPaidBy(currentUserId);
        payment.setPaidAt(LocalDateTime.now());
        RoyaltyPayment saved = royaltyPaymentRepository.save(payment);
        auditService.log(currentUserId, "PAY", "royalty_payments", saved.getId(),
                "{\"status\":\"UNPAID\"}",
                "{\"status\":\"PAID\",\"paidAmount\":" + saved.getPaidAmount() + "}");

        markPeriodPaidIfAllPaid(period, currentUserId);
        return RoyaltyPaymentResponse.from(saved, userReadService.getUserNames().get(saved.getUserId()));
    }

    // ------------------------------------------------------------------ UC-57

    // UC-57: nhuan but cua nguoi dang dang nhap, gom theo ky: cac dong tien (bai theo ty le
    // dong tac gia, anh/video, dieu chinh) + phieu chi neu ky da khoa. Ky moi nhat len dau.
    @Transactional(readOnly = true)
    public List<RoyaltyMyPeriodResponse> getMyRoyalties(Integer currentUserId) {
        Map<Integer, String> titles = loadArticleTitles();
        Map<Integer, RoyaltyMyPeriodResponse> rowsByPeriod = new LinkedHashMap<>();

        for (RoyaltyLine line : royaltyLineRepository.findMyLines(currentUserId)) {
            RoyaltyPeriod period = line.getPeriod();
            RoyaltyMyPeriodResponse row = rowsByPeriod.get(period.getId());
            if (row == null) {
                row = newPeriodRow(period);
                rowsByPeriod.put(period.getId(), row);
            }
            row.getLines().add(RoyaltyMyLineResponse.from(line, titles.get(line.getGrading().getArticleId())));
            row.setTotalAmount(row.getTotalAmount().add(line.getAmount()));
        }

        // Gan phieu chi vao ky; ky chi co phieu 0d (dang no, khong co dong tien) van hien
        String myName = userReadService.getUserNames().get(currentUserId);
        for (RoyaltyPayment payment : royaltyPaymentRepository.findMyPayments(currentUserId)) {
            RoyaltyPeriod period = payment.getPeriod();
            RoyaltyMyPeriodResponse row = rowsByPeriod.get(period.getId());
            if (row == null) {
                row = newPeriodRow(period);
                rowsByPeriod.put(period.getId(), row);
            }
            row.setPayment(RoyaltyPaymentResponse.from(payment, myName));
        }

        List<RoyaltyMyPeriodResponse> result = new ArrayList<>(rowsByPeriod.values());
        result.sort(Comparator.comparing(RoyaltyMyPeriodResponse::getPeriodYear)
                .thenComparing(RoyaltyMyPeriodResponse::getPeriodMonth).reversed());
        return result;
    }

    // ------------------------------------------------------------- ham phu

    // BR-RY-07: moi phieu cua ky da PAID -> ky PAID (ghi paid_at), bao nhung nguoi nhan tien (UC-54 buoc 3).
    // Dem sau khi save trong cung transaction: Hibernate flush truoc khi dem nen dem dung.
    private void markPeriodPaidIfAllPaid(RoyaltyPeriod period, Integer currentUserId) {
        if (royaltyPaymentRepository.countByPeriodIdAndStatus(period.getId(), RoyaltyPaymentStatus.UNPAID) > 0) {
            return;
        }
        period.setStatus(RoyaltyPeriodStatus.PAID);
        period.setPaidAt(LocalDateTime.now());
        royaltyPeriodRepository.save(period);
        auditService.log(currentUserId, "PAY", "royalty_periods", period.getId(),
                "{\"status\":\"LOCKED\"}", "{\"status\":\"PAID\"}");
        for (RoyaltyPayment payment : royaltyPaymentRepository.findByPeriodId(period.getId())) {
            notificationService.notifyUser(payment.getUserId(), null,
                    "Kỳ nhuận bút " + label(period) + " đã chi xong. Xem chi tiết ở mục Nhuận bút của tôi.");
        }
    }

    private static RoyaltyMyPeriodResponse newPeriodRow(RoyaltyPeriod period) {
        RoyaltyMyPeriodResponse row = new RoyaltyMyPeriodResponse();
        row.setPeriodId(period.getId());
        row.setPeriodYear(period.getPeriodYear());
        row.setPeriodMonth(period.getPeriodMonth());
        row.setPeriodStatus(period.getStatus());
        return row;
    }

    private Map<Integer, String> loadArticleTitles() {
        Map<Integer, String> titles = new HashMap<>();
        for (ArticlePublishedResponse article : articlePublishedService.getPublishedArticles()) {
            titles.put(article.getId(), article.getTitle());
        }
        return titles;
    }

    private RoyaltyPeriod findPeriod(Integer periodId) {
        return royaltyPeriodRepository.findById(periodId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy kỳ nhuận bút có ID: " + periodId));
    }

    private static String label(RoyaltyPeriod period) {
        return period.getPeriodMonth() + "/" + period.getPeriodYear();
    }
}
