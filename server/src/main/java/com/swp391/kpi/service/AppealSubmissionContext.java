package com.swp391.kpi.service;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Goi du lieu (bai, diem, tieu chi) ma AppealService can khi kiem tra 1 khieu nai.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   20  field                           repository / service duoc inject
 */
@Getter
@Setter
public class AppealSubmissionContext {

    private Integer articleId;
    private String articleTitle;
    private String articleStatus;
    private Integer mainAuthorId;
    private Integer scoringAttemptId;
    private LocalDateTime announcedAt;
    private BigDecimal totalScore;
    private List<AppealSubmissionCriterion> criteria;
}
