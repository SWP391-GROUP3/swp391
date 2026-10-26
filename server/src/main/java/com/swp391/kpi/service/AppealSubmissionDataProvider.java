package com.swp391.kpi.service;

/**
 * MUC LUC (so dong)
 *   11  getSubmissionContext()
 *   13  getSubmissionContext()
 *   14  getSubmissionContext()
 */
public interface AppealSubmissionDataProvider {

    AppealSubmissionContext getSubmissionContext(Integer articleId);

    default AppealSubmissionContext getSubmissionContext(Integer articleId, Integer attemptId) {
        return getSubmissionContext(articleId);
    }
}
