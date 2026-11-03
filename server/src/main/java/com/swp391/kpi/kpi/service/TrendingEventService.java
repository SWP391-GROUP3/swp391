package com.swp391.kpi.kpi.service;

import com.swp391.kpi.article.entity.Article;
import com.swp391.kpi.article.repository.ArticleRepository;
import com.swp391.kpi.kpi.dto.TrendingEventItemResponse;
import com.swp391.kpi.kpi.dto.TrendingEventReportResponse;
import com.swp391.kpi.kpi.entity.ReaderInteraction;
import com.swp391.kpi.kpi.repository.ReaderInteractionRepository;
import com.swp391.kpi.royalty.entity.Event;
import com.swp391.kpi.royalty.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service xu ly bao cao su kien xu huong va phat hien tang dot bien (UC-65).
 * Thuat toan: Tinh luot xem trong 7 ngay gan nhat [T-6, T] va so sanh voi muc co so
 * trung binh 7 ngay cua 4 tuan truoc do [T-34, T-7].
 *
 * MUC LUC (so dong - ten - tac dung)
 *   33  field                           cac repository va bo tinh toan
 *   42  getTrendingEventsReport()       Lay danh sach su kien va phat hien dot bien
 */
@Service
@RequiredArgsConstructor
public class TrendingEventService {

    private final EventRepository eventRepository;
    private final ArticleRepository articleRepository;
    private final ReaderInteractionRepository readerInteractionRepository;

    private final TrendingEventCalculator calculator = new TrendingEventCalculator();

    /**
     * Lay bao cao phan tich su kien xu huong theo ngay moc (UC-65).
     * Neu khong truyen anchorDate thi mac dinh lay ngay co ban ghi tuong tac doc gia moi nhat
     * hoac ngay hien tai.
     */
    @Transactional(readOnly = true)
    public TrendingEventReportResponse getTrendingEventsReport(LocalDate anchorDate) {
        // 1. Xac dinh ngay moc anchorDate
        LocalDate anchor = anchorDate;
        if (anchor == null) {
            anchor = readerInteractionRepository.findMaxRecordDate();
            if (anchor == null) {
                anchor = LocalDate.now();
            }
        }

        LocalDate recentStart = anchor.minusDays(6);
        LocalDate recentEnd = anchor;
        LocalDate baselineStart = anchor.minusDays(34);
        LocalDate baselineEnd = anchor.minusDays(7);

        // 2. Lay tat ca su kien
        List<Event> events = eventRepository.findAll();

        // 3. Lay tat ca bai viet de gom nhom theo event_id
        List<Article> allArticles = articleRepository.findAll();
        Map<Integer, List<Integer>> eventArticleIdsMap = new HashMap<>();
        for (Article a : allArticles) {
            if (a.getEventId() != null) {
                eventArticleIdsMap.computeIfAbsent(a.getEventId(), k -> new ArrayList<>()).add(a.getId());
            }
        }

        // 4. Duyet tung su kien de tinh toan luot xem
        List<TrendingEventItemResponse> items = new ArrayList<>();
        int surgeCount = 0;

        for (Event event : events) {
            List<Integer> articleIds = eventArticleIdsMap.getOrDefault(event.getId(), Collections.emptyList());

            Map<LocalDate, Long> dailyViewsMap = new HashMap<>();
            if (!articleIds.isEmpty()) {
                List<ReaderInteraction> interactions = readerInteractionRepository.findByArticleIdIn(articleIds);
                for (ReaderInteraction ri : interactions) {
                    if (ri.getRecordDate() != null && ri.getPageViews() != null) {
                        dailyViewsMap.merge(ri.getRecordDate(), (long) ri.getPageViews(), Long::sum);
                    }
                }
            }

            // Tinh toan bang calculator
            long recent7DaysViews = calculator.calculateRecent7DaysViews(dailyViewsMap, anchor);
            BigDecimal baselineWeeklyViews = calculator.calculateBaseline4WeeksAverage(dailyViewsMap, anchor);
            boolean isSurge = calculator.isSurgeDetected(recent7DaysViews, baselineWeeklyViews);
            BigDecimal surgeRatio = calculator.calculateSurgeRatio(recent7DaysViews, baselineWeeklyViews);

            TrendingEventItemResponse item = new TrendingEventItemResponse();
            item.setEventId(event.getId());
            item.setEventName(event.getName());
            item.setStartDate(event.getStartDate());
            item.setEndDate(event.getEndDate());
            item.setIsActive(event.getIsActive() != null ? event.getIsActive() : true);
            item.setArticleCount(articleIds.size());
            item.setRecent7DaysViews(recent7DaysViews);
            item.setBaselineWeeklyViews(baselineWeeklyViews);
            item.setSurgeRatio(surgeRatio);
            item.setSurgeDetected(isSurge);

            if (articleIds.isEmpty() || (recent7DaysViews == 0 && baselineWeeklyViews.compareTo(BigDecimal.ZERO) == 0)) {
                item.setStatusLabel("CHƯA ĐỦ DỮ LIỆU");
            } else if (isSurge) {
                item.setStatusLabel("TĂNG ĐỘT BIẾN");
                surgeCount++;
            } else {
                item.setStatusLabel("BÌNH THƯỜNG");
            }

            items.add(item);
        }

        // Sap xep: su kien tang dot bien len dau, sau do theo luot xem 7 ngay giam dan
        items.sort((a, b) -> {
            if (Boolean.TRUE.equals(a.getSurgeDetected()) && !Boolean.TRUE.equals(b.getSurgeDetected())) {
                return -1;
            }
            if (!Boolean.TRUE.equals(a.getSurgeDetected()) && Boolean.TRUE.equals(b.getSurgeDetected())) {
                return 1;
            }
            return Long.compare(b.getRecent7DaysViews(), a.getRecent7DaysViews());
        });

        TrendingEventReportResponse response = new TrendingEventReportResponse();
        response.setAnchorDate(anchor);
        response.setRecentWindowStart(recentStart);
        response.setRecentWindowEnd(recentEnd);
        response.setBaselineWindowStart(baselineStart);
        response.setBaselineWindowEnd(baselineEnd);
        response.setTotalEvents(items.size());
        response.setSurgeCount(surgeCount);
        response.setItems(items);

        return response;
    }
}
