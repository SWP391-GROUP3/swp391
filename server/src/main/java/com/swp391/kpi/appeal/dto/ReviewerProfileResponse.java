package com.swp391.kpi.appeal.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO tra ve cho giao dien. Dung o: ReviewerProfileController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   12  record                          cac truong khai bao trong ngoac cua record
 */
public record ReviewerProfileResponse(Integer editorId, String editorName, Integer sectionId,
        String sectionName, String status, LocalDate period, long activeArticleCount,
        int pendingCrossReviewCount, long pendingAppealReviewCount, ReviewerDeviationResponse deviation,
        List<ReviewerCrossReviewResponse> crossReviews) {}
