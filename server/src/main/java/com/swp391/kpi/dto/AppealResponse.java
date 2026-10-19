package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MUC LUC (so dong)
 *   17  field                        - cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class AppealResponse {

    private Integer id;
    private Integer articleId;
    private String articleTitle;
    private Integer scoringAttemptId;
    private String status;
    private LocalDateTime submittedAt;
    private List<AppealCriterionResponse> criteria;
}
