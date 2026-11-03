package com.swp391.kpi.kpi.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO nhận dữ liệu khi Trưởng ban điều chỉnh chỉ tiêu đã giao (UC-59 Alternative Flow A2).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class KpiTargetUpdateRequest {

    @NotNull(message = "Số bài xuất bản tối thiểu không được để trống")
    @Min(value = 0, message = "Số bài xuất bản tối thiểu không được âm")
    private Integer minPublished;

    @NotNull(message = "Điểm chất lượng tối thiểu không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm chất lượng tối thiểu phải từ 0 đến 100")
    @DecimalMax(value = "100.0", message = "Điểm chất lượng tối thiểu phải từ 0 đến 100")
    private BigDecimal minAvgScore;
}
