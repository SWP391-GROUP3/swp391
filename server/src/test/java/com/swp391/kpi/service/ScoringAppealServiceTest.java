package com.swp391.kpi.service;

import com.swp391.kpi.entity.*;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScoringAppealServiceTest {
    ScoringAttemptRepository attempts = mock(ScoringAttemptRepository.class);
    ScoringAttemptDetailRepository details = mock(ScoringAttemptDetailRepository.class);
    AuditService audit = mock(AuditService.class);
    UserReadService users = mock(UserReadService.class);
    ScoringAppealService service;
    ScoringAttempt original;

    @BeforeEach void setup() {
        service = new ScoringAppealService(attempts, details, audit, users, new ObjectMapper(),
                mock(jakarta.persistence.EntityManager.class), mock(CriteriaRepository.class), mock(ArticleReadService.class));
        original = new ScoringAttempt(); original.setId(10); original.setArticleId(20);
        original.setVersionId(30); original.setReviewerId(3); original.setAttemptNo(4);
        original.setSubmittedAt(LocalDateTime.now());
        var assignment = new Assignment(); assignment.setId(40); original.setAssignment(assignment);
        var criteria = new CriteriaSetVersion(); criteria.setId(50); original.setCriteriaSetVersion(criteria);
        original.setCategoryId(60);
        when(attempts.findLockedById(10)).thenReturn(Optional.of(original));
        when(attempts.findByArticleIdOrderByAttemptNoAsc(20)).thenReturn(List.of(original));
        when(attempts.save(any())).thenAnswer(call -> { ScoringAttempt a = call.getArgument(0); a.setId(11); return a; });
    }

    @Test void createsBlankAppealWithFrozenOriginalSnapshotWithoutChangingAssignment() {
        var result = service.createDraft(70, 20, 10, 9, true);
        assertEquals(30, result.versionId()); assertEquals(40, result.assignmentId());
        assertEquals(50, result.criteriaSetVersionId()); assertEquals(60, result.categoryId());
        assertEquals(5, result.attemptNo()); assertEquals(9, result.reviewerId());
        assertEquals(70, result.appealId()); assertTrue(result.crossDepartment());
        assertNull(result.submittedAt()); assertEquals(3, original.getReviewerId());
        var saved = org.mockito.ArgumentCaptor.forClass(ScoringAttempt.class);
        verify(attempts).save(saved.capture());
        assertNull(saved.getValue().getDecision()); assertNull(saved.getValue().getTotalScore());
        assertSame(original.getAssignment(), saved.getValue().getAssignment());
    }

    @Test void refusesDuplicateDraftOrWrongOriginalArticle() {
        var previous = new ScoringAttempt(); previous.setAppealId(70);
        when(attempts.findByArticleIdOrderByAttemptNoAsc(20)).thenReturn(List.of(original, previous));
        assertThrows(BusinessException.class, () -> service.createDraft(70, 20, 10, 9, false));
        assertThrows(BusinessException.class, () -> service.createDraft(70, 21, 10, 9, false));
        verify(attempts, never()).save(any());
    }

    @Test void reassignsSameDraftClearsPrivateDraftValuesAndAuditsBeforeChange() {
        original.setAppealId(70); original.setAttemptType(AttemptType.APPEAL); original.setSubmittedAt(null);
        original.setTotalScore(new BigDecimal("45.00")); original.setDecisionNote("Private old note");
        var detail = new ScoringAttemptDetail(); detail.setScore(new BigDecimal("45.00"));
        detail.setComment("Private criterion comment"); var criterion = new Criteria(); criterion.setId(90);
        detail.setCriterion(criterion);
        when(details.findByScoringAttemptId(10)).thenReturn(List.of(detail));
        var result = service.reassignDraft(70, 20, 10, 3, 9, true, "Editor unavailable", 5);
        assertEquals(10, result.id()); assertEquals(4, result.attemptNo()); assertEquals(9, result.reviewerId());
        assertNull(original.getTotalScore()); assertNull(original.getDecisionNote()); assertNull(original.getDecision());
        var oldJson = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(audit).log(eq(5), eq("REASSIGN"), eq("appeals"), eq(70), oldJson.capture(), anyString());
        assertTrue(oldJson.getValue().contains("Private criterion comment"));
        assertTrue(oldJson.getValue().contains("Private old note"));
        verify(details).deleteAll(List.of(detail));
    }

    @Test void rejectsSubmittedForeignOrStaleDraftAndOldOwnerLosesAccess() {
        original.setAppealId(70); original.setAttemptType(AttemptType.APPEAL);
        assertThrows(BusinessException.class, () -> service.reassignDraft(70, 20, 10, 3, 9, false, "reason", 5));
        original.setSubmittedAt(null);
        assertThrows(BusinessException.class, () -> service.reassignDraft(70, 20, 10, 9, 3, false, "reason", 5));
        assertThrows(BusinessException.class, () -> service.reassignDraft(71, 20, 10, 3, 9, false, "reason", 5));
        when(details.findByScoringAttemptId(10)).thenReturn(List.of());
        service.reassignDraft(70, 20, 10, 3, 9, false, "reason", 5);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.requireOwnedDraft(10, 3));
        assertEquals(9, service.requireOwnedDraft(10, 9).reviewerId());
    }
}
