package com.swp391.kpi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Du lieu Admin gui len khi sua he so 1 loai anh/video (UC-56).
 *
 * MUC LUC (so dong)
 *   24  field                        - cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class MediaFactorUpdateRequest {

    @NotNull(message = "Hệ số không được để trống")
    @DecimalMin(value = "0.01", message = "Hệ số phải lớn hơn 0")
    @Digits(integer = 3, fraction = 2, message = "Hệ số tối đa 3 chữ số phần nguyên và 2 chữ số thập phân")
    private BigDecimal factor;
}
