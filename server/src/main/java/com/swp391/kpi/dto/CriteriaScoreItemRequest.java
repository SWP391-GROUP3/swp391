package com.swp391.kpi.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaScoreItemRequest {

    @NotNull(message = "ID tiêu chí (criterionId) không được để trống")
    private Integer criterionId;

    @NotNull(message = "Điểm tiêu chí không được để trống")
    @DecimalMin(value = "0.00", message = "Điểm phải từ 0 trở lên")
    @DecimalMax(value = "100.00", message = "Điểm không được vượt quá 100")
    private BigDecimal score;

    private String comment;
}
