package com.swp391.kpi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Du lieu Ke toan gui len khi them don gia moi (UC-55).
 * Chi nhap luong co so va ngay hieu luc; don gia do he thong tu tinh.
 *
 * MUC LUC (so dong)
 *   26  field                        - cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class RoyaltyRateCreateRequest {

    @NotNull(message = "Lương cơ sở không được để trống")
    @DecimalMin(value = "1", message = "Lương cơ sở phải lớn hơn 0")
    @Digits(integer = 18, fraction = 0, message = "Lương cơ sở phải là số nguyên đồng, tối đa 18 chữ số")
    private BigDecimal baseSalary;

    @NotNull(message = "Ngày hiệu lực không được để trống")
    private LocalDate effectiveFrom;
}
