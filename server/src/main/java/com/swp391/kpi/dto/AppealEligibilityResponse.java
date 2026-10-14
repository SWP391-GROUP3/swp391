package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class AppealEligibilityResponse {

    private Integer articleId;
    private String articleTitle;
    private BigDecimal totalScore;
    private LocalDateTime announcedAt;
    private LocalDateTime appealDeadline;
    private List<AppealScoreCriterionResponse> criteria;
}
