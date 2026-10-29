package com.swp391.kpi.dto;

import com.swp391.kpi.entity.Criteria;
import com.swp391.kpi.entity.ScoringAttemptDetail;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO tra ve cho giao dien. Dung o: ScoringAttemptService.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  field                           cac truong tra ve cho giao dien
 *   30  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
@NoArgsConstructor
public class ScoringAttemptDetailResponse {
    private Integer id;
    private Integer criterionId;
    private String criterionName;
    private BigDecimal weight;
    private Integer sortOrder;
    private BigDecimal score;
    private String comment;

    public static ScoringAttemptDetailResponse from(ScoringAttemptDetail detail) {
        ScoringAttemptDetailResponse res = new ScoringAttemptDetailResponse();
        res.setId(detail.getId());
        res.setScore(detail.getScore());
        res.setComment(detail.getComment());
        if (detail.getCriterion() != null) {
            Criteria c = detail.getCriterion();
            res.setCriterionId(c.getId());
            res.setCriterionName(c.getName());
            res.setWeight(c.getWeight());
            res.setSortOrder(c.getSortOrder());
        }
        return res;
    }
}
