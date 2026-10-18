package com.swp391.kpi.service;

import com.swp391.kpi.dto.ArticlePublishedResponse;
import com.swp391.kpi.dto.RoyaltyPendingArticleResponse;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.repository.RoyaltyGradingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Kiem tra danh sach bai cho cham nhuan but (UC-46), khong can DB.
 */
@ExtendWith(MockitoExtension.class)
class RoyaltyGradingServiceTest {

    @Mock
    private RoyaltyGradingRepository royaltyGradingRepository;

    @Mock
    private ArticlePublishedService articlePublishedService;

    @Mock
    private SystemParameterService systemParameterService;

    @InjectMocks
    private RoyaltyGradingService royaltyGradingService;

    @Test
    void getPendingArticlesSkipsGradedArticlesAndWarnsNearDeadline() {
        LocalDate today = LocalDate.now();
        when(systemParameterService.getValuesByFlow("BF-04"))
                .thenReturn(Map.of("PAYMENT_DEADLINE_DAYS", "30", "PAYMENT_WARN_DAYS", "7"));
        when(articlePublishedService.getPublishedArticles()).thenReturn(List.of(
                publishedArticle(1, today.minusDays(40)),   // da cham -> bi loai
                publishedArticle(2, today.minusDays(25)),   // con 5 ngay -> canh bao
                publishedArticle(3, today.minusDays(2))));  // con 28 ngay -> chua canh bao
        when(royaltyGradingRepository.findGradedArticleIds()).thenReturn(List.of(1));

        List<RoyaltyPendingArticleResponse> result = royaltyGradingService.getPendingArticles();

        assertEquals(2, result.size());
        RoyaltyPendingArticleResponse nearDue = result.get(0);
        assertEquals(Integer.valueOf(2), nearDue.getArticleId());
        assertEquals(today.plusDays(5), nearDue.getPaymentDeadline());
        assertEquals(Long.valueOf(5), nearDue.getDaysLeft());
        assertTrue(nearDue.getNearDeadline());

        RoyaltyPendingArticleResponse fresh = result.get(1);
        assertEquals(Integer.valueOf(3), fresh.getArticleId());
        assertEquals(Long.valueOf(28), fresh.getDaysLeft());
        assertFalse(fresh.getNearDeadline());
    }

    @Test
    void getPendingArticlesMarksOverdueArticleWithNegativeDaysLeft() {
        LocalDate today = LocalDate.now();
        when(systemParameterService.getValuesByFlow("BF-04"))
                .thenReturn(Map.of("PAYMENT_DEADLINE_DAYS", "30", "PAYMENT_WARN_DAYS", "7"));
        when(articlePublishedService.getPublishedArticles()).thenReturn(List.of(publishedArticle(4, today.minusDays(33))));
        when(royaltyGradingRepository.findGradedArticleIds()).thenReturn(List.of());

        RoyaltyPendingArticleResponse overdue = royaltyGradingService.getPendingArticles().get(0);

        assertEquals(Long.valueOf(-3), overdue.getDaysLeft());   // qua han 3 ngay
        assertTrue(overdue.getNearDeadline());
    }

    @Test
    void getPendingArticlesFailsWhenDeadlineParameterIsMissing() {
        when(systemParameterService.getValuesByFlow("BF-04")).thenReturn(Map.of("PAYMENT_WARN_DAYS", "7"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> royaltyGradingService.getPendingArticles());

        assertTrue(error.getMessage().contains("PAYMENT_DEADLINE_DAYS"));
    }

    // Bai da dang gia, chi can id va ngay dang (dang luc 9 gio sang)
    private static ArticlePublishedResponse publishedArticle(Integer id, LocalDate publishedDate) {
        ArticlePublishedResponse article = new ArticlePublishedResponse();
        article.setId(id);
        article.setTitle("Bai " + id);
        article.setPublishedAt(publishedDate.atTime(9, 0));
        return article;
    }
}
