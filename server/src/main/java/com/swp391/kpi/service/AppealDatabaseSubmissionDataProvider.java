package com.swp391.kpi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

/**
 * MUC LUC (so dong)
 *   17  field                        - repository / service duoc inject
 *   22  getSubmissionContext()
 *   28  getSubmissionContext()
 */
@Service
@RequiredArgsConstructor
public class AppealDatabaseSubmissionDataProvider implements AppealSubmissionDataProvider {
    private final ArticleReadService articleReadService;
    private final ScoringReadService scoringReadService;

    @Override
    @Transactional(readOnly = true)
    public AppealSubmissionContext getSubmissionContext(Integer articleId) {
        return getSubmissionContext(articleId, scoringReadService.getPublishedAttempt(articleId).id());
    }

    @Override
    @Transactional(readOnly = true)
    public AppealSubmissionContext getSubmissionContext(Integer articleId, Integer attemptId) {
        var article = articleReadService.getArticle(articleId);
        var attempt = scoringReadService.getAttempt(attemptId);
        if (!articleId.equals(attempt.articleId()))
            throw new com.swp391.kpi.exception.BusinessException("Phiếu chấm không thuộc bài viết.");
        var context = new AppealSubmissionContext();
        context.setArticleId(article.id());
        context.setArticleTitle(article.title());
        context.setArticleStatus(article.status());
        context.setMainAuthorId(article.authorId());
        context.setScoringAttemptId(attempt.id());
        context.setAnnouncedAt(attempt.announcedAt());
        context.setTotalScore(attempt.totalScore());
        var criteria = new ArrayList<AppealSubmissionCriterion>();
        for (var item : scoringReadService.getCriteria(attempt.id())) {
            criteria.add(new AppealSubmissionCriterion(item.criterionId(), item.name(), item.score(), item.comment()));
        }
        context.setCriteria(criteria);
        return context;
    }
}
