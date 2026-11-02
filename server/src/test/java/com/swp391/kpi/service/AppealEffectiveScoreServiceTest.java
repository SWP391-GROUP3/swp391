package com.swp391.kpi.service;

import com.swp391.kpi.dto.AppealEffectiveAttemptResponse;
import com.swp391.kpi.dto.ScoringReadResponse;
import com.swp391.kpi.entity.Appeal;
import com.swp391.kpi.entity.AppealResolution;
import com.swp391.kpi.entity.AppealStatus;
import com.swp391.kpi.repository.AppealRepository;
import com.swp391.kpi.repository.AppealResolutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// Q-D1: nguon diem hieu luc duy nhat (BR-QC-10)
@ExtendWith(MockitoExtension.class)
class AppealEffectiveScoreServiceTest {

    private static final Integer ARTICLE_ID = 7;
    private static final Integer S0_ATTEMPT_ID = 70;

    @Mock private AppealRepository appealRepository;
    @Mock private AppealResolutionRepository resolutionRepository;
    @Mock private ScoringReadService scoringReadService;

    private AppealEffectiveScoreService service;

    @BeforeEach
    void setUp() {
        service = new AppealEffectiveScoreService(appealRepository, resolutionRepository, scoringReadService);
    }

    @Test
    void returnsNullWhenScoreNotPublished() {
        when(scoringReadService.findPublishedAttempt(ARTICLE_ID)).thenReturn(null);

        assertNull(service.getEffectiveAttempt(ARTICLE_ID));
        assertNull(service.getEffectiveScore(ARTICLE_ID));
    }

    @Test
    void usesPublishedScoreWhenNoAppeal() {
        when(scoringReadService.findPublishedAttempt(ARTICLE_ID)).thenReturn(publishedS0("72.50"));
        when(appealRepository.findByArticleId(ARTICLE_ID)).thenReturn(Optional.empty());

        AppealEffectiveAttemptResponse result = service.getEffectiveAttempt(ARTICLE_ID);

        assertEquals(S0_ATTEMPT_ID, result.attemptId());
        assertEquals(new BigDecimal("72.50"), result.score());
        assertFalse(result.fromAppeal());
    }

    @Test
    void keepsPublishedScoreWhileAppealInReview() {
        when(scoringReadService.findPublishedAttempt(ARTICLE_ID)).thenReturn(publishedS0("72.50"));
        when(appealRepository.findByArticleId(ARTICLE_ID)).thenReturn(Optional.of(appeal(AppealStatus.IN_REVIEW)));

        assertEquals(new BigDecimal("72.50"), service.getEffectiveScore(ARTICLE_ID));
    }

    @Test
    void usesConfirmedScoreWhenAppealResolved() {
        when(scoringReadService.findPublishedAttempt(ARTICLE_ID)).thenReturn(publishedS0("72.50"));
        when(appealRepository.findByArticleId(ARTICLE_ID)).thenReturn(Optional.of(appeal(AppealStatus.RESOLVED)));
        AppealResolution resolution = new AppealResolution();
        resolution.setEffectiveAttemptId(71);
        resolution.setEffectiveScore(new BigDecimal("80.25"));
        when(resolutionRepository.findByAppealId(3)).thenReturn(Optional.of(resolution));

        AppealEffectiveAttemptResponse result = service.getEffectiveAttempt(ARTICLE_ID);

        assertEquals(71, result.attemptId());
        assertEquals(new BigDecimal("80.25"), result.score());
        assertTrue(result.fromAppeal());
    }

    private ScoringReadResponse publishedS0(String score) {
        return new ScoringReadResponse(S0_ATTEMPT_ID, ARTICLE_ID, 5, "INITIAL", new BigDecimal(score),
                LocalDateTime.of(2026, 10, 30, 9, 0), LocalDateTime.of(2026, 10, 30, 10, 0));
    }

    private Appeal appeal(AppealStatus status) {
        Appeal appeal = new Appeal();
        appeal.setId(3);
        appeal.setStatus(status);
        return appeal;
    }
}
