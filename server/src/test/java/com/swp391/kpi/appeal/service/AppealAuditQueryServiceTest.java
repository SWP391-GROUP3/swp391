package com.swp391.kpi.appeal.service;
import com.swp391.kpi.admin.dto.UserAccessResponse;
import com.swp391.kpi.admin.service.UserReadService;
import com.swp391.kpi.appeal.entity.Appeal;
import com.swp391.kpi.appeal.repository.AppealCriterionRepository;
import com.swp391.kpi.appeal.repository.AppealRepository;
import com.swp391.kpi.appeal.repository.AppealResolutionRepository;
import com.swp391.kpi.appeal.repository.CrossReviewTaskRepository;
import com.swp391.kpi.article.dto.ArticleReadResponse;
import com.swp391.kpi.article.service.ArticleReadService;
import com.swp391.kpi.assessment.service.ScoringReadService;
import com.swp391.kpi.common.entity.AuditLog;
import com.swp391.kpi.common.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppealAuditQueryServiceTest {
    private final AuditLogRepository logs = mock(AuditLogRepository.class);
    private final UserReadService users = mock(UserReadService.class);
    private final ArticleReadService articles = mock(ArticleReadService.class);
    private final ScoringReadService scores = mock(ScoringReadService.class);
    private final AppealRepository appeals = mock(AppealRepository.class);
    private final AppealAuditQueryService service = new AppealAuditQueryService(logs, users, articles, scores,
            appeals, mock(AppealCriterionRepository.class), mock(AppealResolutionRepository.class),
            mock(CrossReviewTaskRepository.class), new com.fasterxml.jackson.databind.ObjectMapper());

    @Test void headCannotSeeOtherSectionOrGlobalConfigurationEvents() {
        when(users.requireActive(5, "ADMIN", "HEAD_OF_DEPARTMENT")).thenReturn(
                new UserAccessResponse(5, "Head", "HEAD_OF_DEPARTMENT", 1, "Ban 1", "ACTIVE"));
        var own = appeal(1, 1);
        var other = appeal(2, 2);
        when(appeals.findById(1)).thenReturn(Optional.of(own));
        when(appeals.findById(2)).thenReturn(Optional.of(other));
        when(articles.getArticle(1)).thenReturn(new ArticleReadResponse(1, 1, 1, "Own", "PUBLISHED"));
        when(articles.getArticle(2)).thenReturn(new ArticleReadResponse(2, 7, 2, "Other", "PUBLISHED"));
        when(logs.search(null, null, null, null, null, null)).thenReturn(
                List.of(log(1, "appeals", 1), log(2, "appeals", 2), log(3, "appeal_rule_configs", 1)));
        var result = service.getLogs(5, null, null, null, null, null, null, null, null, null);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getId());
    }

    @Test void directOutOfSectionArticleReturns403() {
        when(users.requireActive(5, "ADMIN", "HEAD_OF_DEPARTMENT")).thenReturn(
                new UserAccessResponse(5, "Head", "HEAD_OF_DEPARTMENT", 1, "Ban 1", "ACTIVE"));
        when(articles.getArticle(2)).thenReturn(new ArticleReadResponse(2, 7, 2, "Other", "PUBLISHED"));
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.getLogs(5, null, null, null, null, null, null, 2, null, null));
        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(logs);
    }

    @Test void adminCanReadSystemEvents() {
        when(users.requireActive(8, "ADMIN", "HEAD_OF_DEPARTMENT")).thenReturn(
                new UserAccessResponse(8, "Admin", "ADMIN", null, null, "ACTIVE"));
        when(logs.search(null, null, null, null, null, null)).thenReturn(
                List.of(log(1, "appeal_rule_configs", 1)));
        assertEquals("Hệ thống", service.getLogs(8, null, null, null, null, null, null, null, null, null)
                .get(0).getActorDisplay());
    }
    private Appeal appeal(int id, int articleId) {
        var a = new Appeal(); a.setId(id); a.setArticleId(articleId); return a;
    }
    private AuditLog log(int id, String type, int entityId) {
        var log = new AuditLog(); log.setId(id); log.setEntityType(type); log.setEntityId(entityId);
        log.setCreatedAt(LocalDateTime.now()); log.setActionType("CREATE"); return log;
    }
}
