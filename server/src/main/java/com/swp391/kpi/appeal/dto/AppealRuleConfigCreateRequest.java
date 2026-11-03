package com.swp391.kpi.appeal.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO client gui len API. Dung o: AppealRuleConfigController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   25  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class AppealRuleConfigCreateRequest {

    @NotNull(message = "Thời hạn khiếu nại không được để trống")
    @Min(value = 1, message = "Thời hạn khiếu nại phải lớn hơn 0")
    private Integer appealWindowDays;

    @NotNull(message = "Số lần khiếu nại tối đa không được để trống")
    @Min(value = 1, message = "Số lần khiếu nại tối đa phải lớn hơn 0")
    private Integer maxAppealsPerArticle;

    @NotNull(message = "Ngưỡng chênh lệch điểm không được để trống")
    @DecimalMin(value = "0.00", message = "Ngưỡng chênh lệch điểm phải từ 0 đến 100")
    @DecimalMax(value = "100.00", message = "Ngưỡng chênh lệch điểm phải từ 0 đến 100")
    @jakarta.validation.constraints.Digits(integer = 3, fraction = 2,
            message = "Ngưỡng chênh lệch chỉ được có tối đa hai chữ số thập phân")
    private BigDecimal varianceThreshold;

    @NotNull(message = "Ngày hiệu lực không được để trống")
    private LocalDate effectiveFrom;
}
