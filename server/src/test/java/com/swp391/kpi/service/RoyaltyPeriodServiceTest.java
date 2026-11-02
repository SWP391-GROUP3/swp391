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
import com.swp391.kpi.repository.RoyaltyGradingRepository;
import com.swp391.kpi.repository.RoyaltyLineRepository;
import com.swp391.kpi.repository.RoyaltyPaymentRepository;
import com.swp391.kpi.repository.RoyaltyPeriodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiem tra ky nhuan but (UC-49, UC-50, UC-52, UC-53), khong can DB.
 */
@ExtendWith(MockitoExtension.class)
class RoyaltyPeriodServiceTest {

    private static final Integer SECRETARY_ID = 4;
    private static final Integer ACCOUNTANT_ID = 5;

    @Mock private RoyaltyPeriodRepository royaltyPeriodRepository;
    @Mock private RoyaltyGradingRepository royaltyGradingRepository;
    @Mock private RoyaltyLineRepository royaltyLineRepository;
    @Mock private RoyaltyPaymentRepository royaltyPaymentRepository;
    @Mock private RoyaltyGradingService royaltyGradingService;
    @Mock private ArticlePublishedService articlePublishedService;
    @Mock private UserReadService userReadService;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;

    @InjectMocks
    private RoyaltyPeriodService royaltyPeriodService;

    // ------------------------------------------------------------------ UC-49

    @Test
    void getAllPeriodsAddsTotalAmountAndGradedArticleCount() {
        RoyaltyPeriod october = period(3, 2026, 10, RoyaltyPeriodStatus.OPEN);
        when(royaltyPeriodRepository.findAllByOrderByPeriodYearDescPeriodMonthDesc()).thenReturn(List.of(october));
        when(royaltyPeriodRepository.sumLineAmountByPeriodId(3)).thenReturn(new BigDecimal("1233000"));
        when(royaltyPeriodRepository.countGradedArticlesByPeriodId(3)).thenReturn(2);

        List<RoyaltyPeriodResponse> result = royaltyPeriodService.getAllPeriods();

        assertEquals(1, result.size());
        RoyaltyPeriodResponse period = result.get(0);
        assertEquals(RoyaltyPeriodStatus.OPEN, period.getStatus());
        assertEquals(Byte.valueOf((byte) 10), period.getPeriodMonth());
        assertMoney("1233000", period.getTotalAmount());
        assertEquals(Integer.valueOf(2), period.getGradedArticleCount());
    }

    // ---------------------------------------------------- UC-50 bang ke ky

    @Test
    void getPeriodDetailSumsLinesPerPersonAndPerArticle() {
        // Giong data.sql sau khi cham bai 5: bai 1 (phieu 1) va bai 5 (phieu 2) cung ky 9/2026
        RoyaltyGrading article1 = grading(1, 1, "900000");
        RoyaltyGrading article5 = grading(2, 5, "966000");
        when(royaltyPeriodRepository.findById(1)).thenReturn(Optional.of(period(1, 2026, 9, RoyaltyPeriodStatus.OPEN)));
        when(royaltyGradingRepository.findByPeriodIdOrderByGradedAtAsc(1)).thenReturn(List.of(article1, article5));
        when(royaltyLineRepository.findByPeriodId(1)).thenReturn(List.of(
                line(article1, 1, RoyaltyLineType.ARTICLE, "630000"),
                line(article1, 2, RoyaltyLineType.ARTICLE, "270000"),
                line(article1, 2, RoyaltyLineType.MEDIA, "234000"),
                line(article5, 1, RoyaltyLineType.ARTICLE, "580000"),
                line(article5, 2, RoyaltyLineType.ARTICLE, "386000"),
                line(article5, 2, RoyaltyLineType.MEDIA, "234000")));
        when(articlePublishedService.getPublishedArticles()).thenReturn(List.of(published(1, "Bai 1"), published(5, "Bai 5")));
        when(userReadService.getUserNames()).thenReturn(java.util.Map.of(1, "Nguyen Van An", 2, "Tran Thi Binh"));

        RoyaltyPeriodDetailResponse detail = royaltyPeriodService.getPeriodDetail(1);

        assertMoney("2334000", detail.getTotalAmount());
        assertEquals(Integer.valueOf(2), detail.getGradedArticleCount());

        // Bai 5: tien bai 966.000 + anh 234.000 = 1.200.000
        RoyaltyPeriodArticleResponse row5 = detail.getArticles().get(1);
        assertEquals("Bai 5", row5.getTitle());
        assertMoney("234000", row5.getMediaAmount());
        assertMoney("1200000", row5.getTotalAmount());

        // An: 630.000 + 580.000; Binh: bai 270.000 + 386.000, anh 234.000 x 2
        RoyaltyPeriodPersonResponse an = detail.getPeople().get(0);
        assertEquals("Nguyen Van An", an.getFullName());
        assertMoney("1210000", an.getTotalAmount());
        RoyaltyPeriodPersonResponse binh = detail.getPeople().get(1);
        assertMoney("656000", binh.getArticleAmount());
        assertMoney("468000", binh.getMediaAmount());
        assertMoney("1124000", binh.getTotalAmount());
    }

    // ------------------------------------------------------- UC-50 chot ky

    @Test
    void closePeriodClosesOpenPeriodCreatesNextMonthAndNotifiesAccountant() {
        RoyaltyPeriod september = period(1, 2026, 9, RoyaltyPeriodStatus.OPEN);
        when(royaltyPeriodRepository.findById(1)).thenReturn(Optional.of(september));
        // Bai dang 05/10 chua cham: sang thang sau nen KHONG chan chot ky 9
        when(royaltyGradingService.getPendingArticles()).thenReturn(List.of(pending(6, 2026, 10, 5)));
        when(royaltyPeriodRepository.save(any(RoyaltyPeriod.class))).thenAnswer(call -> call.getArgument(0));
        when(royaltyPeriodRepository.existsByPeriodYearAndPeriodMonth((short) 2026, (byte) 10)).thenReturn(false);

        RoyaltyPeriodResponse result = royaltyPeriodService.closePeriod(1, SECRETARY_ID);

        assertEquals(RoyaltyPeriodStatus.CLOSED, result.getStatus());
        // Lan save 1: ky 9 chuyen CLOSED; lan save 2: ky 10 moi, OPEN
        ArgumentCaptor<RoyaltyPeriod> captor = ArgumentCaptor.forClass(RoyaltyPeriod.class);
        verify(royaltyPeriodRepository, times(2)).save(captor.capture());
        RoyaltyPeriod october = captor.getAllValues().get(1);
        assertEquals(Short.valueOf((short) 2026), october.getPeriodYear());
        assertEquals(Byte.valueOf((byte) 10), october.getPeriodMonth());
        assertEquals(RoyaltyPeriodStatus.OPEN, october.getStatus());

        verify(notificationService).notifyRole(eq("ACCOUNTANT"), contains("9/2026"));
        verify(auditService).log(eq(SECRETARY_ID), eq("CLOSE"), eq("royalty_periods"), eq(1), any(), any());
    }

    @Test
    void closePeriodInDecemberCreatesJanuaryOfNextYear() {
        when(royaltyPeriodRepository.findById(12)).thenReturn(Optional.of(period(12, 2026, 12, RoyaltyPeriodStatus.OPEN)));
        when(royaltyGradingService.getPendingArticles()).thenReturn(List.of());
        when(royaltyPeriodRepository.save(any(RoyaltyPeriod.class))).thenAnswer(call -> call.getArgument(0));
        when(royaltyPeriodRepository.existsByPeriodYearAndPeriodMonth((short) 2027, (byte) 1)).thenReturn(false);

        royaltyPeriodService.closePeriod(12, SECRETARY_ID);

        ArgumentCaptor<RoyaltyPeriod> captor = ArgumentCaptor.forClass(RoyaltyPeriod.class);
        verify(royaltyPeriodRepository, times(2)).save(captor.capture());
        RoyaltyPeriod january = captor.getAllValues().get(1);
        assertEquals(Short.valueOf((short) 2027), january.getPeriodYear());
        assertEquals(Byte.valueOf((byte) 1), january.getPeriodMonth());
    }

    @Test
    void closePeriodDoesNotCreateNextPeriodWhenItAlreadyExists() {
        when(royaltyPeriodRepository.findById(1)).thenReturn(Optional.of(period(1, 2026, 9, RoyaltyPeriodStatus.OPEN)));
        when(royaltyGradingService.getPendingArticles()).thenReturn(List.of());
        when(royaltyPeriodRepository.save(any(RoyaltyPeriod.class))).thenAnswer(call -> call.getArgument(0));
        when(royaltyPeriodRepository.existsByPeriodYearAndPeriodMonth((short) 2026, (byte) 10)).thenReturn(true);

        royaltyPeriodService.closePeriod(1, SECRETARY_ID);

        verify(royaltyPeriodRepository, times(1)).save(any(RoyaltyPeriod.class));   // chi luu ky 9
    }

    @Test
    void closePeriodFailsWhenArticlePublishedInMonthIsNotGraded() {
        when(royaltyPeriodRepository.findById(1)).thenReturn(Optional.of(period(1, 2026, 9, RoyaltyPeriodStatus.OPEN)));
        // Bai 4 dang 22/09 chua cham -> chan chot ky 9 (UC-50 E1)
        when(royaltyGradingService.getPendingArticles()).thenReturn(List.of(pending(4, 2026, 9, 22)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> royaltyPeriodService.closePeriod(1, SECRETARY_ID));

        assertTrue(error.getMessage().contains("Còn 1 bài"));
        verify(royaltyPeriodRepository, never()).save(any(RoyaltyPeriod.class));
        verify(notificationService, never()).notifyRole(any(), any());
    }

    @Test
    void closePeriodFailsWhenPeriodIsNotOpen() {
        when(royaltyPeriodRepository.findById(1)).thenReturn(Optional.of(period(1, 2026, 9, RoyaltyPeriodStatus.CLOSED)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> royaltyPeriodService.closePeriod(1, SECRETARY_ID));

        assertTrue(error.getMessage().contains("Chỉ chốt được kỳ đang mở"));
        verify(royaltyPeriodRepository, never()).save(any(RoyaltyPeriod.class));
    }

    // ------------------------------------------------------- UC-52 tra ky

    @Test
    void returnPeriodReopensClosedPeriodWithNoteAndNotifiesSecretary() {
        RoyaltyPeriod september = period(1, 2026, 9, RoyaltyPeriodStatus.CLOSED);
        when(royaltyPeriodRepository.findById(1)).thenReturn(Optional.of(september));
        when(royaltyPeriodRepository.save(any(RoyaltyPeriod.class))).thenAnswer(call -> call.getArgument(0));

        RoyaltyPeriodResponse result = royaltyPeriodService.returnPeriod(1, "  Sai tỷ lệ bài 5  ", ACCOUNTANT_ID);

        assertEquals(RoyaltyPeriodStatus.OPEN, result.getStatus());
        assertEquals("Sai tỷ lệ bài 5", result.getReturnNote());   // bo khoang trang 2 dau
        assertEquals(ACCOUNTANT_ID, september.getReturnedBy());
        verify(notificationService).notifyRole(eq("EDITORIAL_SECRETARY"), contains("9/2026"));
        verify(auditService).log(eq(ACCOUNTANT_ID), eq("RETURN"), eq("royalty_periods"), eq(1), any(), any());
    }

    @Test
    void returnPeriodFailsWhenPeriodIsNotClosed() {
        // Ky OPEN chua chot va ky LOCKED da khoa deu khong tra duoc (BR-RY-07)
        when(royaltyPeriodRepository.findById(1)).thenReturn(Optional.of(period(1, 2026, 9, RoyaltyPeriodStatus.LOCKED)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> royaltyPeriodService.returnPeriod(1, "Sai tỷ lệ", ACCOUNTANT_ID));

        assertTrue(error.getMessage().contains("Chỉ trả lại được kỳ đã chốt"));
        verify(royaltyPeriodRepository, never()).save(any(RoyaltyPeriod.class));
        verify(notificationService, never()).notifyRole(any(), any());
    }

    // ------------------------------------------------------- UC-53 khoa ky

    @Test
    void lockPeriodCreatesOneUnpaidPaymentPerPersonAndAttachesLines() {
        RoyaltyPeriod september = period(1, 2026, 9, RoyaltyPeriodStatus.CLOSED);
        RoyaltyGrading article5 = grading(2, 5, "966000");
        RoyaltyLine anArticle = line(article5, 1, RoyaltyLineType.ARTICLE, "580000");
        RoyaltyLine binhArticle = line(article5, 2, RoyaltyLineType.ARTICLE, "386000");
        RoyaltyLine binhMedia = line(article5, 2, RoyaltyLineType.MEDIA, "234000");
        when(royaltyPeriodRepository.findById(1)).thenReturn(Optional.of(september));
        // Ky 8/2026 khong co: ky dau tien, chua ai no
        when(royaltyPeriodRepository.findByPeriodYearAndPeriodMonth((short) 2026, (byte) 8)).thenReturn(Optional.empty());
        when(royaltyLineRepository.findByPeriodId(1)).thenReturn(List.of(anArticle, binhArticle, binhMedia));
        when(royaltyPaymentRepository.save(any(RoyaltyPayment.class))).thenAnswer(call -> call.getArgument(0));
        when(royaltyPeriodRepository.save(any(RoyaltyPeriod.class))).thenAnswer(call -> call.getArgument(0));

        RoyaltyPeriodResponse result = royaltyPeriodService.lockPeriod(1, ACCOUNTANT_ID);

        assertEquals(RoyaltyPeriodStatus.LOCKED, result.getStatus());
        assertEquals(ACCOUNTANT_ID, september.getLockedBy());
        assertNotNull(september.getLockedAt());

        // 2 phieu, xep theo id nguoi: An 580.000, Binh 386.000 + 234.000
        ArgumentCaptor<RoyaltyPayment> captor = ArgumentCaptor.forClass(RoyaltyPayment.class);
        verify(royaltyPaymentRepository, times(2)).save(captor.capture());
        RoyaltyPayment an = captor.getAllValues().get(0);
        assertEquals(Integer.valueOf(1), an.getUserId());
        assertMoney("580000", an.getGrossAmount());
        assertMoney("0", an.getDebtIn());
        assertMoney("580000", an.getPaidAmount());
        assertEquals(RoyaltyPaymentStatus.UNPAID, an.getStatus());
        assertNull(an.getPaidBy());
        RoyaltyPayment binh = captor.getAllValues().get(1);
        assertMoney("620000", binh.getGrossAmount());
        assertMoney("620000", binh.getPaidAmount());

        // Moi dong tien gan dung phieu cua nguoi nhan
        assertSame(an, anArticle.getPayment());
        assertSame(binh, binhArticle.getPayment());
        assertSame(binh, binhMedia.getPayment());
        verify(auditService).log(eq(ACCOUNTANT_ID), eq("LOCK"), eq("royalty_periods"), eq(1), any(), any());
    }

    @Test
    void lockPeriodCarriesDebtFromPreviousPeriodAndCreatesZeroPaymentForDebtorWithoutLines() {
        RoyaltyPeriod october = period(2, 2026, 10, RoyaltyPeriodStatus.CLOSED);
        RoyaltyPeriod september = period(1, 2026, 9, RoyaltyPeriodStatus.LOCKED);
        when(royaltyPeriodRepository.findById(2)).thenReturn(Optional.of(october));
        when(royaltyPeriodRepository.findByPeriodYearAndPeriodMonth((short) 2026, (byte) 9)).thenReturn(Optional.of(september));
        // Ky 9: An con no 200.000, Cuong con no 50.000, Binh het no
        when(royaltyPaymentRepository.findByPeriodId(1)).thenReturn(List.of(
                payment(1, "200000"), payment(2, "0"), payment(3, "50000")));
        when(royaltyLineRepository.findByPeriodId(2)).thenReturn(List.of(
                line(grading(3, 6, "500000"), 1, RoyaltyLineType.ARTICLE, "500000")));
        when(royaltyPaymentRepository.save(any(RoyaltyPayment.class))).thenAnswer(call -> call.getArgument(0));
        when(royaltyPeriodRepository.save(any(RoyaltyPeriod.class))).thenAnswer(call -> call.getArgument(0));

        royaltyPeriodService.lockPeriod(2, ACCOUNTANT_ID);

        ArgumentCaptor<RoyaltyPayment> captor = ArgumentCaptor.forClass(RoyaltyPayment.class);
        verify(royaltyPaymentRepository, times(2)).save(captor.capture());
        // An: 500.000 - no 200.000 = chi 300.000, het no
        RoyaltyPayment an = captor.getAllValues().get(0);
        assertEquals(Integer.valueOf(1), an.getUserId());
        assertMoney("200000", an.getDebtIn());
        assertMoney("300000", an.getPaidAmount());
        assertMoney("0", an.getDebtOut());
        // Cuong: ky 10 khong co dong tien nhung van co phieu 0d, giu no 50.000
        RoyaltyPayment cuong = captor.getAllValues().get(1);
        assertEquals(Integer.valueOf(3), cuong.getUserId());
        assertMoney("0", cuong.getGrossAmount());
        assertMoney("0", cuong.getPaidAmount());
        assertMoney("50000", cuong.getDebtOut());
    }

    @Test
    void lockPeriodFailsWhenPreviousPeriodIsNotLocked() {
        when(royaltyPeriodRepository.findById(2)).thenReturn(Optional.of(period(2, 2026, 10, RoyaltyPeriodStatus.CLOSED)));
        // Ky 9 bi Ke toan tra ve, dang OPEN lai -> chua biet no cua ky 9
        when(royaltyPeriodRepository.findByPeriodYearAndPeriodMonth((short) 2026, (byte) 9))
                .thenReturn(Optional.of(period(1, 2026, 9, RoyaltyPeriodStatus.OPEN)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> royaltyPeriodService.lockPeriod(2, ACCOUNTANT_ID));

        assertTrue(error.getMessage().contains("Kỳ 9/2026 chưa khoá"));
        verify(royaltyPaymentRepository, never()).save(any(RoyaltyPayment.class));
        verify(royaltyPeriodRepository, never()).save(any(RoyaltyPeriod.class));
    }

    @Test
    void lockPeriodFailsWhenPeriodIsNotClosed() {
        when(royaltyPeriodRepository.findById(1)).thenReturn(Optional.of(period(1, 2026, 9, RoyaltyPeriodStatus.OPEN)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> royaltyPeriodService.lockPeriod(1, ACCOUNTANT_ID));

        assertTrue(error.getMessage().contains("Chỉ khoá được kỳ đã chốt"));
        verify(royaltyPaymentRepository, never()).save(any(RoyaltyPayment.class));
    }

    // ------------------------------------------- bang ke sau khi cham lai

    @Test
    void getPeriodDetailShowsOnlyLatestVersionOfRegradedArticle() {
        RoyaltyGrading version1 = grading(2, 5, "966000");
        RoyaltyGrading version2 = grading(4, 5, "1050000");
        version2.setVersionNo(2);
        when(royaltyPeriodRepository.findById(1)).thenReturn(Optional.of(period(1, 2026, 9, RoyaltyPeriodStatus.OPEN)));
        when(royaltyGradingRepository.findByPeriodIdOrderByGradedAtAsc(1)).thenReturn(List.of(version1, version2));
        // Dong tien ban 1 da bi xoa luc cham lai, chi con dong cua ban 2
        when(royaltyLineRepository.findByPeriodId(1)).thenReturn(List.of(
                line(version2, 1, RoyaltyLineType.ARTICLE, "1050000")));
        when(articlePublishedService.getPublishedArticles()).thenReturn(List.of(published(5, "Bai 5")));
        when(userReadService.getUserNames()).thenReturn(java.util.Map.of(1, "Nguyen Van An"));

        RoyaltyPeriodDetailResponse detail = royaltyPeriodService.getPeriodDetail(1);

        assertEquals(1, detail.getArticles().size());
        assertMoney("1050000", detail.getArticles().get(0).getTotalAmount());
        assertEquals(Integer.valueOf(1), detail.getGradedArticleCount());
    }

    // ----------------------------------------------------------- du lieu gia

    private static RoyaltyPeriod period(Integer id, int year, int month, RoyaltyPeriodStatus status) {
        RoyaltyPeriod period = new RoyaltyPeriod();
        period.setId(id);
        period.setPeriodYear((short) year);
        period.setPeriodMonth((byte) month);
        period.setStatus(status);
        return period;
    }

    private static RoyaltyGrading grading(Integer id, Integer articleId, String amount) {
        RoyaltyGrading grading = new RoyaltyGrading();
        grading.setId(id);
        grading.setArticleId(articleId);
        grading.setVersionNo(1);
        grading.setAmount(new BigDecimal(amount));
        return grading;
    }

    // Phieu chi da khoa cua ky truoc, chi can nguoi nhan va no chuyen sang ky sau
    private static RoyaltyPayment payment(Integer userId, String debtOut) {
        RoyaltyPayment payment = new RoyaltyPayment();
        payment.setUserId(userId);
        payment.setDebtOut(new BigDecimal(debtOut));
        return payment;
    }

    private static RoyaltyLine line(RoyaltyGrading grading, Integer userId, RoyaltyLineType type, String amount) {
        RoyaltyLine line = new RoyaltyLine();
        line.setGrading(grading);
        line.setUserId(userId);
        line.setLineType(type);
        line.setAmount(new BigDecimal(amount));
        return line;
    }

    private static ArticlePublishedResponse published(Integer id, String title) {
        ArticlePublishedResponse article = new ArticlePublishedResponse();
        article.setId(id);
        article.setTitle(title);
        return article;
    }

    // Bai cho cham, chi can ngay dang (luc 9 gio sang)
    private static RoyaltyPendingArticleResponse pending(Integer articleId, int year, int month, int day) {
        RoyaltyPendingArticleResponse article = new RoyaltyPendingArticleResponse();
        article.setArticleId(articleId);
        article.setPublishedAt(LocalDateTime.of(year, month, day, 9, 0));
        return article;
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "mong doi " + expected + " nhung la " + actual);
    }
}
