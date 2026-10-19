package com.swp391.kpi.service;
import com.swp391.kpi.dto.*;
import com.swp391.kpi.entity.*;
import com.swp391.kpi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewerDeviationRefreshServiceTest {
    private final UserReadService users = mock(UserReadService.class);
    private final ScoringReadService scores = mock(ScoringReadService.class);
    private final AppealRepository appeals = mock(AppealRepository.class);
    private final AppealResolutionRepository resolutions = mock(AppealResolutionRepository.class);
    private final CrossReviewTaskRepository cross = mock(CrossReviewTaskRepository.class);
    private final ReviewerDeviationRepository indices = mock(ReviewerDeviationRepository.class);
    private final ReviewerDeviationRefreshService service = new ReviewerDeviationRefreshService(
            users, scores, appeals, resolutions, cross, indices);
    private final LocalDate month = LocalDate.of(2026, 10, 1);
    private final LocalDateTime submitted = month.atStartOfDay();

    @BeforeEach void setUp() {
        when(users.requireActive(8, "ADMIN", "HEAD_OF_DEPARTMENT"))
                .thenReturn(new UserAccessResponse(8, "Admin", "ADMIN", null, null, "ACTIVE"));
    }

    @Test void combinesCrossReviewsAndConfirmedNumericAppealScoreForOriginalReviewer() {
        var original = new ScoringReadResponse(1, 1, 3, "INITIAL", new BigDecimal("70"), submitted, submitted);
        var blind = new ScoringReadResponse(2, 1, 9, "APPEAL", new BigDecimal("80"), submitted, null);
        when(scores.getSubmittedAttempts()).thenReturn(List.of(original, blind));
        var task = new CrossReviewTask();
        task.setStatus(CrossReviewTaskStatus.DONE);
        task.setOriginalScoringId(1);
        task.setScore(new BigDecimal("65"));
        task.setSubmittedAt(submitted);
        when(cross.findAll()).thenReturn(List.of(task));
        var appeal = new Appeal();
        appeal.setId(1); appeal.setStatus(AppealStatus.RESOLVED); appeal.setScoringAttemptId(1);
        when(appeals.findById(1)).thenReturn(Optional.of(appeal));
        var resolution = new AppealResolution();
        resolution.setAppealId(1); resolution.setFinalizedAt(submitted);
        resolution.setEffectiveScore(new BigDecimal("77.50"));
        when(resolutions.findAll()).thenReturn(List.of(resolution));
        when(indices.findAllByOrderByPeriodDescEditorIdAsc()).thenReturn(List.of());

        assertEquals(Set.of(3), service.refresh(month, 8));

        var captor = ArgumentCaptor.forClass(ReviewerDeviationIndex.class);
        verify(indices).save(captor.capture());
        var index = captor.getValue();
        assertEquals(3, index.getEditorId());
        assertEquals(new BigDecimal("6.25"), index.getAvgDeviation());
        assertEquals(1, index.getAppealCount());
        assertEquals(new BigDecimal("100.00"), index.getOverturnRate());
        assertEquals(1, index.getWorkloadCount());
    }

    @Test void noComparisonsDoesNotPersistIndex() {
        when(scores.getSubmittedAttempts()).thenReturn(List.of());
        when(cross.findAll()).thenReturn(List.of());
        when(resolutions.findAll()).thenReturn(List.of());
        when(indices.findAllByOrderByPeriodDescEditorIdAsc()).thenReturn(List.of());
        assertTrue(service.refresh(month, 8).isEmpty());
        verify(indices, never()).save(any());
    }

    @Test void rejectsNonFirstDayBeforeReadingScores() {
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.refresh(month.plusDays(1), 8));
        assertEquals(400, error.getStatusCode().value());
        verifyNoInteractions(scores, indices);
    }
}
