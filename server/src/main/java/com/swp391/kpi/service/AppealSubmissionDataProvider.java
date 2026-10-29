package com.swp391.kpi.service;

/**
 * Interface lay du lieu can cho khieu nai, de AppealService khong goi thang repository cua luong khac.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   13  getSubmissionContext()          lay submission context
 *   15  getSubmissionContext()          lay submission context
 *   16  getSubmissionContext()          lay submission context
 */
public interface AppealSubmissionDataProvider {

    AppealSubmissionContext getSubmissionContext(Integer articleId);

    default AppealSubmissionContext getSubmissionContext(Integer articleId, Integer attemptId) {
        return getSubmissionContext(articleId);
    }
}
