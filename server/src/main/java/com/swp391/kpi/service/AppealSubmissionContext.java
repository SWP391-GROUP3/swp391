package com.swp391.kpi.service;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
