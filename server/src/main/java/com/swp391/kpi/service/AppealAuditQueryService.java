package com.swp391.kpi.service;

import com.swp391.kpi.dto.AuditLogResponse;
import com.swp391.kpi.entity.AuditLog;
import com.swp391.kpi.entity.Appeal;
import com.swp391.kpi.entity.CrossReviewTask;
import com.swp391.kpi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class AppealAuditQueryService {
    private final AuditLogRepository auditLogRepository;
    private final UserReadService userReadService;
    private final ArticleReadService articleReadService;
    private final ScoringReadService scoringReadService;
    private final AppealRepository appealRepository;
    private final AppealCriterionRepository criterionRepository;
    private final AppealResolutionRepository resolutionRepository;
    private final CrossReviewTaskRepository crossReviewTaskRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper json;

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogs(Integer currentUserId, Integer actorId, String actionType,
            String entityType, Integer entityId, LocalDateTime fromTime, LocalDateTime toTime,
            Integer articleId, Integer appealId, Integer editorId) {
        var user = userReadService.requireActive(currentUserId, "ADMIN", "HEAD_OF_DEPARTMENT");
        if (fromTime != null && toTime != null && fromTime.isAfter(toTime))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        if (appealId != null) {
            var appeal = appealRepository.findById(appealId)
                    .orElseThrow(() -> new com.swp391.kpi.exception.NotFoundException("Không tìm thấy khiếu nại."));
            if (articleId != null && !articleId.equals(appeal.getArticleId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài viết không khớp khiếu nại.");
            articleId = appeal.getArticleId();
        }
        if (articleId != null && cannotView(user.role(), user.sectionId(), articleId))
            throw forbidden();
        List<AuditLogResponse> result = new ArrayList<>();
        for (var log : auditLogRepository.search(actorId, blankToNull(actionType), blankToNull(entityType),
                entityId, fromTime, toTime)) {
            Integer logArticleId = findArticle(log);
            if (!"ADMIN".equals(user.role()) && cannotView(user.role(), user.sectionId(), logArticleId)) {
                if (entityId != null && entityType != null) throw forbidden();
                continue;
            }
            if (articleId != null && !articleId.equals(logArticleId)) continue;
            if (appealId != null && !matchesAppeal(log, appealId)) continue;
            if (editorId != null && !matchesEditor(log, logArticleId, editorId)) continue;
            result.add(AuditLogResponse.from(log));
        }
        return result;
    }

    private Integer findArticle(AuditLog log) {
        return switch (log.getEntityType()) {
            case "articles" -> log.getEntityId();
            case "appeals" -> appealRepository.findById(log.getEntityId()).map(Appeal::getArticleId).orElse(null);
            case "appeal_criteria" -> criterionRepository.findById(log.getEntityId())
                    .map(c -> c.getAppeal().getArticleId()).orElse(null);
            case "appeal_resolutions" -> resolutionRepository.findById(log.getEntityId())
                    .flatMap(r -> appealRepository.findById(r.getAppealId())).map(Appeal::getArticleId).orElse(null);
            case "scoring_attempts" -> scoringReadService.getAttempt(log.getEntityId()).articleId();
            case "cross_review_tasks" -> crossReviewTaskRepository.findById(log.getEntityId())
                    .map(CrossReviewTask::getArticleId).orElse(null);
            default -> null;
        };
    }

    private boolean matchesAppeal(AuditLog log, Integer appealId) {
        if ("scoring_attempts".equals(log.getEntityType()))
            return appealId.equals(scoringReadService.getAttempt(log.getEntityId()).appealId());
        if ("appeals".equals(log.getEntityType())) return appealId.equals(log.getEntityId());
        if ("appeal_criteria".equals(log.getEntityType()))
            return criterionRepository.findById(log.getEntityId())
                    .map(c -> appealId.equals(c.getAppeal().getId())).orElse(false);
        if ("appeal_resolutions".equals(log.getEntityType()))
            return resolutionRepository.findById(log.getEntityId())
                    .map(r -> appealId.equals(r.getAppealId())).orElse(false);
        return false;
    }

    private boolean matchesEditor(AuditLog log, Integer articleId, Integer editorId) {
        if (editorId.equals(log.getActorId())) return true;
        if ("appeals".equals(log.getEntityType()) && ("ASSIGN".equals(log.getActionType())
                || "REASSIGN".equals(log.getActionType()))) {
            if (hasReviewer(log.getOldValue(), editorId) || hasReviewer(log.getNewValue(), editorId)) return true;
        }
        if (articleId == null) return false;
        for (var attempt : scoringReadService.getSubmittedAttempts()) {
            if (articleId.equals(attempt.articleId()) && editorId.equals(attempt.reviewerId())) return true;
        }
        return false;
    }

    private boolean hasReviewer(String value, Integer editorId) {
        if (value == null) return false;
        try {
            var reviewer = json.readTree(value).get("reviewerId");
            return reviewer != null && reviewer.isIntegralNumber() && reviewer.intValue() == editorId;
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) { return false; }
    }

    private boolean cannotView(String role, Integer sectionId, Integer articleId) {
        if ("ADMIN".equals(role)) return false;
        return articleId == null || sectionId == null
                || !sectionId.equals(articleReadService.getArticle(articleId).sectionId());
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem nhật ký này.");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
