package com.swp391.kpi.dto;

import com.swp391.kpi.entity.ReviewerDeviationIndex;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MUC LUC (so dong)
 *   20  field                        - cac truong tra ve cho giao dien
 *   32  from()                       - doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class ReviewerDeviationResponse {

    private Integer id;
    private Integer editorId;
    private String editorName;
    private Integer sectionId;
    private String sectionName;
    private LocalDate period;
    private BigDecimal avgDeviation;
    private Integer appealCount;
    private BigDecimal overturnRate;
    private Integer workloadCount;
    private LocalDateTime lastUpdated;

    public static ReviewerDeviationResponse from(
            ReviewerDeviationIndex entity,
            String editorName,
            Integer sectionId,
            String sectionName
    ) {
        ReviewerDeviationResponse response = new ReviewerDeviationResponse();
        response.setId(entity.getId());
        response.setEditorId(entity.getEditorId());
        response.setEditorName(editorName);
        response.setSectionId(sectionId);
        response.setSectionName(sectionName);
        response.setPeriod(entity.getPeriod());
        response.setAvgDeviation(entity.getAvgDeviation());
        response.setAppealCount(entity.getAppealCount());
        response.setOverturnRate(entity.getOverturnRate());
        response.setWorkloadCount(entity.getWorkloadCount());
        response.setLastUpdated(entity.getLastUpdated());
        return response;
    }
}
