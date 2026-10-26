package com.swp391.kpi.service;

import com.swp391.kpi.dto.*;
import com.swp391.kpi.entity.*;
import com.swp391.kpi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewerProfileServiceTest {
    private final UserReadService users = mock(UserReadService.class);
    private final ScoringReadService scores = mock(ScoringReadService.class);
    private final AssignmentService assignments = mock(AssignmentService.class);
    private final ArticleReadService articles = mock(ArticleReadService.class);
    private final AppealRepository appeals = mock(AppealRepository.class);
    private final AppealResolutionRepository resolutions = mock(AppealResolutionRepository.class);
    private final CrossReviewTaskRepository cross = mock(CrossReviewTaskRepository.class);
    private final ScoringAppealService appealScores = mock(ScoringAppealService.class);
    private final ReviewerProfileService service = new ReviewerProfileService(
            users, scores, assignments, articles, appeals, resolutions, cross, appealScores);
    private final LocalDate month = LocalDate.of(2026, 10, 1);

    @BeforeEach void setUp() {
        when(users.requireActive(5, "ADMIN", "HEAD_OF_DEPARTMENT")).thenReturn(
                new UserAccessResponse(5, "Head", "HEAD_OF_DEPARTMENT", 1, "Ban 1", "ACTIVE"));
        when(users.getUsers()).thenReturn(List.of(
                new UserAccessResponse(3, "Editor", "EDITOR", 1, "Ban 1", "ACTIVE"),
                new UserAccessResponse(9, "Other", "EDITOR", 2, "Ban 2", "LOCKED")));
    }

    @Test void profileDoesNotInventDeviationAndCountsOnlyUnfinishedOriginalAssignments() {
        var first = new AssignmentResponse(); first.setArticleId(1);
        var second = new AssignmentResponse(); second.setArticleId(2);
        when(assignments.getAssignmentsByEditorId(3)).thenReturn(List.of(first, second));
        when(articles.getArticle(1)).thenReturn(new ArticleReadResponse(1, 1, 1, "Doing", "ASSIGNED"));
        when(articles.getArticle(2)).thenReturn(new ArticleReadResponse(2, 1, 1, "Done", "PUBLISHED"));
        var result = service.getProfile(3, month, 5);
        assertEquals("Editor", result.editorName());
        assertEquals(1, result.activeArticleCount());
        assertNull(result.deviation());
        assertTrue(result.crossReviews().isEmpty());
    }

    @Test void headCannotReadOtherDepartmentBeforeReadingScoringData() {
        var error = assertThrows(ResponseStatusException.class, () -> service.getProfile(9, month, 5));
        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(scores, assignments, articles, appeals, resolutions, cross);
    }

    @Test void adminMayInspectLockedEditorInOtherDepartment() {
        when(users.requireActive(8, "ADMIN", "HEAD_OF_DEPARTMENT")).thenReturn(
                new UserAccessResponse(8, "Admin", "ADMIN", null, null, "ACTIVE"));
        assertEquals("LOCKED", service.getProfile(9, month, 8).status());
    }

    @Test void directoryIsScopedAndDoesNotRequireDeviationData() {
        assertEquals(List.of(3), service.getEditors(5).stream().map(ReviewerDirectoryResponse::editorId).toList());
        when(users.requireActive(8, "ADMIN", "HEAD_OF_DEPARTMENT")).thenReturn(
                new UserAccessResponse(8, "Admin", "ADMIN", null, null, "ACTIVE"));
        assertEquals(2, service.getEditors(8).size());
        verifyNoInteractions(scores, assignments, articles, appeals, resolutions, cross);
    }

    @Test void historyFiltersEditorAndMonthThenPaginatesNewestFirst() {
        when(scores.getSubmittedAttempts()).thenReturn(List.of(
                score(1, 3, month.plusDays(1)), score(2, 3, month.plusDays(2)),
                score(3, 9, month.plusDays(3)), score(4, 3, month.minusDays(1))));
        when(articles.getArticle(1)).thenReturn(new ArticleReadResponse(1, 1, 1, "Article", "PUBLISHED"));
        var page = service.getHistory(3, month, 0, 1, 5);
        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getContent().get(0).attemptId());
        assertEquals(1, service.getHistory(3, month, 1, 1, 5).getContent().get(0).attemptId());
        assertTrue(service.getHistory(3, month, 8, 1, 5).isEmpty());
    }

    @Test void missingEditorAndInvalidPeriodOrPageAreRejected() {
        assertThrows(com.swp391.kpi.exception.NotFoundException.class, () -> service.getProfile(999, month, 5));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.getProfile(3, month.plusDays(1), 5)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.getHistory(3, month, 0, 101, 5)).getStatusCode().value());
    }

    @Test void profileCalculatesAbsoluteDeviationWithoutWritingIndexes() {
        when(scores.getSubmittedAttempts()).thenReturn(List.of(score(1, 3, month.plusDays(1))));
        var task = new CrossReviewTask(); task.setId(1); task.setOriginalScoringId(1);
        task.setArticleId(1); task.setReviewerId(9); task.setStatus(CrossReviewTaskStatus.DONE);
        task.setScore(new BigDecimal("65")); task.setDeviation(new BigDecimal("-5"));
        task.setSubmittedAt(month.plusDays(2).atStartOfDay());
        when(cross.findAll()).thenReturn(List.of(task));
        var result = service.getProfile(3, month, 5);
        assertEquals(new BigDecimal("5.00"), result.deviation().getAvgDeviation());
        assertEquals(new BigDecimal("0.00"), result.deviation().getOverturnRate());
        verify(cross, never()).save(any());
        verify(appeals, never()).save(any());
    }

    @Test void profileUsesConfirmedNumericAppealScoreAndCombinesAbsoluteComparisons() {
        when(scores.getSubmittedAttempts()).thenReturn(List.of(score(1, 3, month.plusDays(1))));
        var task = new CrossReviewTask(); task.setOriginalScoringId(1); task.setReviewerId(9);
        task.setStatus(CrossReviewTaskStatus.DONE); task.setScore(new BigDecimal("65"));
        task.setSubmittedAt(month.plusDays(1).atStartOfDay());
        when(cross.findAll()).thenReturn(List.of(task));
        var appeal = new Appeal(); appeal.setId(10); appeal.setScoringAttemptId(1); appeal.setStatus(AppealStatus.RESOLVED);
        when(appeals.findById(10)).thenReturn(Optional.of(appeal));
        var resolution = new AppealResolution(); resolution.setAppealId(10);
        resolution.setEffectiveScore(new BigDecimal("77.50")); resolution.setFinalizedAt(month.plusDays(2).atStartOfDay());
        when(resolutions.findAll()).thenReturn(List.of(resolution));
        var result = service.getProfile(3, month, 5);
        assertEquals(new BigDecimal("6.25"), result.deviation().getAvgDeviation());
        assertEquals(1, result.deviation().getAppealCount());
        assertEquals(new BigDecimal("100.00"), result.deviation().getOverturnRate());
        assertEquals(1, result.deviation().getWorkloadCount());
    }

    @Test void crossReviewHistoryFiltersReviewerAndMonthButPendingCountIsCurrent() {
        var pending = new CrossReviewTask(); pending.setId(1); pending.setReviewerId(3); pending.setStatus(CrossReviewTaskStatus.PENDING);
        var done = new CrossReviewTask(); done.setId(2); done.setReviewerId(3); done.setStatus(CrossReviewTaskStatus.DONE);
        done.setSubmittedAt(month.plusDays(1).atStartOfDay());
        var old = new CrossReviewTask(); old.setId(3); old.setReviewerId(3); old.setStatus(CrossReviewTaskStatus.DONE);
        old.setSubmittedAt(month.minusDays(1).atStartOfDay());
        var other = new CrossReviewTask(); other.setId(4); other.setReviewerId(9); other.setStatus(CrossReviewTaskStatus.PENDING);
        when(cross.findAll()).thenReturn(List.of(pending, done, old, other));
        var result = service.getProfile(3, month, 5);
        assertEquals(1, result.pendingCrossReviewCount());
        assertEquals(List.of(2), result.crossReviews().stream().map(ReviewerCrossReviewResponse::id).toList());
    }

    private ScoringReadResponse score(int id, int editorId, LocalDate date) {
        return new ScoringReadResponse(id, 1, editorId, "INITIAL", new BigDecimal("70"), date.atStartOfDay(), null);
    }
}
