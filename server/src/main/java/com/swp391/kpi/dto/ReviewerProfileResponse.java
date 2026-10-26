package com.swp391.kpi.dto;

import java.time.LocalDate;
import java.util.List;

public record ReviewerProfileResponse(Integer editorId, String editorName, Integer sectionId,
        String sectionName, String status, LocalDate period, long activeArticleCount,
        int pendingCrossReviewCount, long pendingAppealReviewCount, ReviewerDeviationResponse deviation,
        List<ReviewerCrossReviewResponse> crossReviews) {}
