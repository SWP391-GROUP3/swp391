package com.swp391.kpi.appeal.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO tra ve cho giao dien. Dung o: CrossReviewController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   12  record                          cac truong khai bao trong ngoac cua record
 */
public record CrossReviewCandidateResponse(Integer originalScoringId, Integer articleId, String title,
        LocalDateTime submittedAt, List<AppealReviewerCandidateResponse> reviewers) {}
