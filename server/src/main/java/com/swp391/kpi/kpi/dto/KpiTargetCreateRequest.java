package com.swp391.kpi.kpi.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO nhận dữ liệu khi Trưởng ban giao chỉ tiêu mới cho phóng viên (UC-59).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class KpiTargetCreateRequest {

    @NotNull(message = "ID phóng viên không được để trống")
    private Integer reporterId;

    @NotNull(message = "Năm chỉ tiêu không được để trống")
    @Min(value = 2000, message = "Năm chỉ tiêu phải từ năm 2000 trở đi")
    private Short targetYear;

    @NotNull(message = "Tháng chỉ tiêu không được để trống")
    @Min(value = 1, message = "Tháng chỉ tiêu phải từ 1 đến 12")
    @Max(value = 12, message = "Tháng chỉ tiêu phải từ 1 đến 12")
    private Byte targetMonth;

    @NotNull(message = "Số bài xuất bản tối thiểu không được để trống")
    @Min(value = 0, message = "Số bài xuất bản tối thiểu không được âm")
    private Integer minPublished;

    @NotNull(message = "Điểm chất lượng tối thiểu không được để trống")
    @DecimalMin(value = "0.0", message = "Điểm chất lượng tối thiểu phải từ 0 đến 100")
    @DecimalMax(value = "100.0", message = "Điểm chất lượng tối thiểu phải từ 0 đến 100")
    private BigDecimal minAvgScore;
}
