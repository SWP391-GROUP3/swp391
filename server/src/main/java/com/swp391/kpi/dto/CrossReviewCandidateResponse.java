package com.swp391.kpi.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CrossReviewCandidateResponse(Integer originalScoringId, Integer articleId, String title,
        LocalDateTime submittedAt, List<AppealReviewerCandidateResponse> reviewers) {}
