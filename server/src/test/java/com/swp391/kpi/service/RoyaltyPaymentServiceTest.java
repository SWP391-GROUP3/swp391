package com.swp391.kpi.service;

import com.swp391.kpi.dto.ArticlePublishedResponse;
import com.swp391.kpi.dto.RoyaltyMyPeriodResponse;
import com.swp391.kpi.dto.RoyaltyPaymentResponse;
import com.swp391.kpi.entity.RoyaltyGrading;
import com.swp391.kpi.entity.RoyaltyLine;
import com.swp391.kpi.entity.RoyaltyLineType;
import com.swp391.kpi.entity.RoyaltyPayment;
import com.swp391.kpi.entity.RoyaltyPaymentStatus;
import com.swp391.kpi.entity.RoyaltyPeriod;
import com.swp391.kpi.entity.RoyaltyPeriodStatus;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.repository.RoyaltyLineRepository;
import com.swp391.kpi.repository.RoyaltyPaymentRepository;
import com.swp391.kpi.repository.RoyaltyPeriodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// UC-54 ghi nhan chi, UC-57 nhuan but cua toi
@ExtendWith(MockitoExtension.class)
class RoyaltyPaymentServiceTest {

    private static final Integer ACCOUNTANT_ID = 6;
    private static final Integer REPORTER_ID = 3;

    @Mock private RoyaltyPaymentRepository royaltyPaymentRepository;
    @Mock private RoyaltyPeriodRepository royaltyPeriodRepository;
    @Mock private RoyaltyLineRepository royaltyLineRepository;
    @Mock private ArticlePublishedService articlePublishedService;
    @Mock private UserReadService userReadService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    @InjectMocks
    private RoyaltyPaymentService royaltyPaymentService;

    @Test
    void recordPaymentMarksVoucherPaidButKeepsPeriodLockedWhileOthersUnpaid() {
        RoyaltyPeriod period = period(RoyaltyPeriodStatus.LOCKED);
        RoyaltyPayment payment = unpaid(period);
        stubPayment(payment);
        when(royaltyPaymentRepository.countByPeriodIdAndStatus(1, RoyaltyPaymentStatus.UNPAID)).thenReturn(1L);

        RoyaltyPaymentResponse result = royaltyPaymentService.recordPayment(20, ACCOUNTANT_ID);

        assertEquals(RoyaltyPaymentStatus.PAID, result.getStatus());
        assertEquals(ACCOUNTANT_ID, result.getPaidBy());
        assertNotNull(result.getPaidAt());
        assertEquals(RoyaltyPeriodStatus.LOCKED, period.getStatus());   // con nguoi chua chi
        verify(royaltyPeriodRepository, never()).save(any());
    }

    @Test
    void recordPaymentMarksPeriodPaidWhenLastVoucherIsPaid() {
        RoyaltyPeriod period = period(RoyaltyPeriodStatus.LOCKED);
        RoyaltyPayment payment = unpaid(period);
        stubPayment(payment);
        when(royaltyPaymentRepository.countByPeriodIdAndStatus(1, RoyaltyPaymentStatus.UNPAID)).thenReturn(0L);
        when(royaltyPaymentRepository.findByPeriodId(1)).thenReturn(List.of(payment));

        royaltyPaymentService.recordPayment(20, ACCOUNTANT_ID);

        assertEquals(RoyaltyPeriodStatus.PAID, period.getStatus());   // BR-RY-07
        assertNotNull(period.getPaidAt());
        verify(royaltyPeriodRepository).save(period);
        verify(notificationService).notifyUser(eq(REPORTER_ID), isNull(), anyString());
    }

    @Test
    void recordPaymentFailsWhenVoucherAlreadyPaid() {
        RoyaltyPayment payment = unpaid(period(RoyaltyPeriodStatus.LOCKED));
        payment.setStatus(RoyaltyPaymentStatus.PAID);
        when(royaltyPaymentRepository.findById(20)).thenReturn(Optional.of(payment));

        BusinessException error = assertThrows(BusinessException.class,
                () -> royaltyPaymentService.recordPayment(20, ACCOUNTANT_ID));

        assertEquals("Phiếu chi này đã được chi, không chi lại được.", error.getMessage());
        verify(royaltyPaymentRepository, never()).save(any());
    }

    @Test
    void recordPaymentFailsWhenPeriodNotLocked() {
        RoyaltyPayment payment = unpaid(period(RoyaltyPeriodStatus.CLOSED));
        when(royaltyPaymentRepository.findById(20)).thenReturn(Optional.of(payment));

        assertThrows(BusinessException.class, () -> royaltyPaymentService.recordPayment(20, ACCOUNTANT_ID));
        verify(royaltyPaymentRepository, never()).save(any());
    }

    @Test
    void myRoyaltiesGroupsLinesOfOnePeriodWithItsVoucher() {
        RoyaltyPeriod period = period(RoyaltyPeriodStatus.LOCKED);
        when(royaltyLineRepository.findByUserIdOrderByCreatedAtDesc(REPORTER_ID)).thenReturn(List.of(
                line(period, 11, RoyaltyLineType.ARTICLE, "580000"),
                line(period, 12, RoyaltyLineType.MEDIA, "234000")));
        when(royaltyPaymentRepository.findByUserId(REPORTER_ID)).thenReturn(List.of(unpaid(period)));
        when(articlePublishedService.getPublishedArticles()).thenReturn(List.of(article(11, "Bài A"), article(12, "Bài B")));
        when(userReadService.getUserNames()).thenReturn(Map.of(REPORTER_ID, "Phóng viên A"));

        List<RoyaltyMyPeriodResponse> result = royaltyPaymentService.getMyRoyalties(REPORTER_ID);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).getLines().size());
        assertEquals("Bài A", result.get(0).getLines().get(0).getArticleTitle());
        assertEquals(0, new BigDecimal("814000").compareTo(result.get(0).getTotalAmount()));
        assertEquals(RoyaltyPaymentStatus.UNPAID, result.get(0).getPayment().getStatus());
    }

    private void stubPayment(RoyaltyPayment payment) {
        when(royaltyPaymentRepository.findById(20)).thenReturn(Optional.of(payment));
        when(royaltyPaymentRepository.save(payment)).thenReturn(payment);
        lenient().when(userReadService.getUserNames()).thenReturn(Map.of(REPORTER_ID, "Phóng viên A"));
    }

    private RoyaltyPeriod period(RoyaltyPeriodStatus status) {
        RoyaltyPeriod period = new RoyaltyPeriod();
        period.setId(1);
        period.setPeriodYear((short) 2026);
        period.setPeriodMonth((byte) 10);
        period.setStatus(status);
        return period;
    }

    private RoyaltyPayment unpaid(RoyaltyPeriod period) {
        RoyaltyPayment payment = new RoyaltyPayment();
        payment.setId(20);
        payment.setPeriod(period);
        payment.setUserId(REPORTER_ID);
        payment.setGrossAmount(new BigDecimal("814000"));
        payment.setDebtIn(BigDecimal.ZERO);
        payment.setPaidAmount(new BigDecimal("814000"));
        payment.setDebtOut(BigDecimal.ZERO);
        payment.setStatus(RoyaltyPaymentStatus.UNPAID);
        return payment;
    }

    private RoyaltyLine line(RoyaltyPeriod period, int articleId, RoyaltyLineType type, String amount) {
        RoyaltyGrading grading = new RoyaltyGrading();
        grading.setArticleId(articleId);
        RoyaltyLine line = new RoyaltyLine();
        line.setPeriod(period);
        line.setGrading(grading);
        line.setUserId(REPORTER_ID);
        line.setLineType(type);
        line.setAmount(new BigDecimal(amount));
        return line;
    }

    private ArticlePublishedResponse article(int id, String title) {
        ArticlePublishedResponse article = new ArticlePublishedResponse();
        article.setId(id);
        article.setTitle(title);
        return article;
    }
}
