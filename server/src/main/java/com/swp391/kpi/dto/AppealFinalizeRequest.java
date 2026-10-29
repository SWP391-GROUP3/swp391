package com.swp391.kpi.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO client gui len API. Dung o: AppealReviewController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   17  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter @Setter
public class AppealFinalizeRequest {
    @NotNull(message = "Thiếu mã phiếu dùng xác nhận kết quả")
    @Positive(message = "Mã phiếu phải là số dương")
    private Integer expectedAttemptId;
}
