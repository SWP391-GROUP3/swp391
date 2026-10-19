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
import com.swp391.kpi.repository.RoyaltyGradingRepository;
import com.swp391.kpi.repository.RoyaltyLineRepository;
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
 * Kiem tra ky nhuan but (UC-49, UC-50), khong can DB.
 */
@ExtendWith(MockitoExtension.class)
class RoyaltyPeriodServiceTest {

    private static final Integer SECRETARY_ID = 4;

    @Mock private RoyaltyPeriodRepository royaltyPeriodRepository;
    @Mock private RoyaltyGradingRepository royaltyGradingRepository;
    @Mock private RoyaltyLineRepository royaltyLineRepository;
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
        when(userReadService.getUsers()).thenReturn(List.of(
                new UserAccessResponse(1, "Nguyen Van An", "REPORTER", 1, "Ban Thoi su", "ACTIVE"),
                new UserAccessResponse(2, "Tran Thi Binh", "REPORTER", 1, "Ban Thoi su", "ACTIVE")));

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
        grading.setAmount(new BigDecimal(amount));
        return grading;
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
