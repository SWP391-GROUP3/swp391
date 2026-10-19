package com.swp391.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaScoreInput {

    private Integer criterionId;
    private String criterionName;
    private BigDecimal weight;   // Ví dụ: 25.00 (%)
    private BigDecimal score;    // Ví dụ: 85.00 (thang 0-100)
    private String comment;
}
