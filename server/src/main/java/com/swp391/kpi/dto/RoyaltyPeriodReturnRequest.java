package com.swp391.kpi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Ke toan tra ky da chot ve cho Thu ky (UC-52): bat buoc ghi chu
 * (cot royalty_periods.return_note NVARCHAR(1000), CHECK ck_royalty_periods_returned).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class RoyaltyPeriodReturnRequest {

    @NotBlank(message = "Vui lòng nhập lý do trả lại kỳ")
    @Size(max = 1000, message = "Lý do tối đa {max} ký tự")
    private String note;
}
