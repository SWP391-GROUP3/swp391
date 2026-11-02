package com.swp391.kpi.service;

import com.swp391.kpi.dto.TrendingEventReportResponse;
import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.Event;
import com.swp391.kpi.entity.ReaderInteraction;
import com.swp391.kpi.repository.ArticleRepository;
import com.swp391.kpi.repository.EventRepository;
import com.swp391.kpi.repository.ReaderInteractionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrendingEventService - Kiểm thử xử lý báo cáo sự kiện xu hướng (UC-65)")
class TrendingEventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ReaderInteractionRepository readerInteractionRepository;

    @InjectMocks
    private TrendingEventService trendingEventService;

    private final LocalDate anchorDate = LocalDate.of(2026, 9, 20);

    @Test
    @DisplayName("Lấy báo cáo sự kiện xu hướng và phát hiện sự kiện tăng đột biến")
    void testGetTrendingEventsReportWithSurge() {
        Event event1 = new Event();
        event1.setId(1);
        event1.setName("Bão số 3 năm 2026");
        event1.setStartDate(LocalDate.of(2026, 9, 5));
        event1.setEndDate(LocalDate.of(2026, 9, 20));
        event1.setIsActive(true);

        when(eventRepository.findAll()).thenReturn(Collections.singletonList(event1));

        Article article1 = new Article();
        article1.setId(101);
        article1.setEventId(1);

        when(articleRepository.findAll()).thenReturn(Collections.singletonList(article1));

        // Reader interactions:
        // Cửa sổ 4 tuần trước: [anchorDate - 34: 2026-08-17] -> [anchorDate - 7: 2026-09-13]
        // Tuần 1..4: tổng 4000 views -> baseline trung bình tuần = 1000
        ReaderInteraction baseInteraction = new ReaderInteraction();
        baseInteraction.setArticleId(101);
        baseInteraction.setRecordDate(LocalDate.of(2026, 9, 1));
        baseInteraction.setPageViews(4000);

        // 7 ngày gần nhất: [2026-09-14 -> 2026-09-20]: 3000 views (> 2x baseline = 2000)
        ReaderInteraction recentInteraction = new ReaderInteraction();
        recentInteraction.setArticleId(101);
        recentInteraction.setRecordDate(LocalDate.of(2026, 9, 18));
        recentInteraction.setPageViews(3000);

        when(readerInteractionRepository.findByArticleIdIn(Collections.singletonList(101)))
                .thenReturn(Arrays.asList(baseInteraction, recentInteraction));

        TrendingEventReportResponse response = trendingEventService.getTrendingEventsReport(anchorDate);

        assertNotNull(response);
        assertEquals(anchorDate, response.getAnchorDate());
        assertEquals(1, response.getTotalEvents());
        assertEquals(1, response.getSurgeCount());

        var item = response.getItems().get(0);
        assertEquals(1, item.getEventId());
        assertEquals("Bão số 3 năm 2026", item.getEventName());
        assertEquals(3000L, item.getRecent7DaysViews());
        assertTrue(item.getSurgeDetected());
        assertEquals("TĂNG ĐỘT BIẾN", item.getStatusLabel());
    }
}
