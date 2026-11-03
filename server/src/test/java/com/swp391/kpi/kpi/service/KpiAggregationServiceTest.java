package com.swp391.kpi.kpi.service;

import com.swp391.kpi.admin.entity.User;
import com.swp391.kpi.admin.repository.UserRepository;
import com.swp391.kpi.appeal.service.AppealEffectiveScoreService;
import com.swp391.kpi.article.entity.Article;
import com.swp391.kpi.article.entity.ArticleAuthor;
import com.swp391.kpi.article.entity.ArticleStatus;
import com.swp391.kpi.article.repository.ArticleAuthorRepository;
import com.swp391.kpi.article.repository.ArticleRepository;
import com.swp391.kpi.kpi.dto.ReporterKpiDetailResponse;
import com.swp391.kpi.kpi.entity.KpiTarget;
import com.swp391.kpi.kpi.repository.KpiSnapshotRepository;
import com.swp391.kpi.kpi.repository.KpiTargetRepository;
import com.swp391.kpi.kpi.repository.ReaderInteractionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

// Q-A3: KPI dung diem hieu luc cua tung bai (BR-KPI-02, BR-QC-10), khong de null nua
@ExtendWith(MockitoExtension.class)
class KpiAggregationServiceTest {

    private static final Integer REPORTER_ID = 5;
    private static final Short YEAR = 2026;
    private static final Byte MONTH = 10;

    @Mock private KpiSnapshotRepository kpiSnapshotRepository;
    @Mock private KpiTargetRepository kpiTargetRepository;
    @Mock private ArticleRepository articleRepository;
    @Mock private ArticleAuthorRepository articleAuthorRepository;
    @Mock private ReaderInteractionRepository readerInteractionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AppealEffectiveScoreService appealEffectiveScoreService;

    @InjectMocks
    private KpiAggregationService kpiAggregationService;

    @Test
    void reporterDetailUsesEffectiveScoreOfEachArticle() {
        User reporter = new User();
        reporter.setId(REPORTER_ID);
        reporter.setFullName("Phóng viên A");
        when(userRepository.findById(REPORTER_ID)).thenReturn(Optional.of(reporter));

        KpiTarget target = new KpiTarget();
        target.setMinPublished(2);
        target.setMinAvgScore(new BigDecimal("75.00"));
        when(kpiTargetRepository.findByReporterIdAndTargetYearAndTargetMonth(REPORTER_ID, YEAR, MONTH))
                .thenReturn(Optional.of(target));
        when(kpiSnapshotRepository.findByReporterIdAndPeriodYearAndPeriodMonth(REPORTER_ID, YEAR, MONTH))
                .thenReturn(Optional.empty());

        when(articleRepository.findAll()).thenReturn(List.of(published(1), published(2)));
        when(articleAuthorRepository.findByUserIdAndArticleIdIn(REPORTER_ID, List.of(1, 2)))
                .thenReturn(List.of(author(1), author(2)));
        when(readerInteractionRepository.sumPageViewsByArticleIds(anyList())).thenReturn(List.of());
        // bai 2 co khieu nai da chot 90 diem thay cho 70 diem goc
        when(appealEffectiveScoreService.getEffectiveScore(1)).thenReturn(new BigDecimal("72.00"));
        when(appealEffectiveScoreService.getEffectiveScore(2)).thenReturn(new BigDecimal("90.00"));

        ReporterKpiDetailResponse detail = kpiAggregationService.getReporterKpiDetail(REPORTER_ID, YEAR, MONTH, REPORTER_ID);

        assertEquals(new BigDecimal("72.00"), detail.getArticles().get(0).getEffectiveScore());
        assertEquals(new BigDecimal("90.00"), detail.getArticles().get(1).getEffectiveScore());
        // (72 + 90) / 2 = 81 >= 75 va du 2 bai -> PASS (BR-KPI-05)
        assertEquals(0, new BigDecimal("81").compareTo(detail.getActualAvgScore()));
        assertEquals("PASS", detail.getProjectedStatus());
    }

    // UC-60: phong vien chi xem KPI cua chinh minh
    @Test
    void reporterCannotViewKpiOfAnotherReporter() {
        com.swp391.kpi.admin.entity.Role reporterRole = new com.swp391.kpi.admin.entity.Role();
        reporterRole.setCode(com.swp391.kpi.admin.entity.RoleCode.REPORTER);
        User otherReporter = new User();
        otherReporter.setId(99);
        otherReporter.setRole(reporterRole);
        when(userRepository.findById(99)).thenReturn(Optional.of(otherReporter));

        org.springframework.web.server.ResponseStatusException error = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> kpiAggregationService.getReporterKpiDetail(REPORTER_ID, YEAR, MONTH, 99));

        assertEquals(403, error.getStatusCode().value());
    }

    private Article published(int id) {
        Article article = new Article();
        article.setId(id);
        article.setTitle("Bài " + id);
        article.setStatus(ArticleStatus.PUBLISHED);
        article.setPublishedAt(LocalDateTime.of(2026, 10, 10 + id, 9, 0));
        return article;
    }

    private ArticleAuthor author(int articleId) {
        ArticleAuthor author = new ArticleAuthor();
        author.setArticleId(articleId);
        author.setUserId(REPORTER_ID);
        author.setSharePercent(new BigDecimal("100.00"));
        return author;
    }
}
