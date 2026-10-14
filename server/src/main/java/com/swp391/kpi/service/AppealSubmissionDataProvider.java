package com.swp391.kpi.service;

public interface AppealSubmissionDataProvider {

    AppealSubmissionContext getSubmissionContext(Integer articleId);

    default AppealSubmissionContext getSubmissionContext(Integer articleId, Integer attemptId) {
        return getSubmissionContext(articleId);
    }
}
