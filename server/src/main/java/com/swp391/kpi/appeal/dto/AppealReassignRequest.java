package com.swp391.kpi.appeal.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO client gui len API. Dung o: AppealManagementController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   16  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter @Setter
public class AppealReassignRequest {
    @NotNull(message = "Thiếu mã phiếu chấm.") @Positive(message = "Mã phiếu không hợp lệ.")
    private Integer attemptId;
    @NotNull(message = "Thiếu người đang được giao.") @Positive(message = "Mã người chấm không hợp lệ.")
    private Integer expectedReviewerId;
    @NotNull(message = "Phải chọn biên tập viên mới.") @Positive(message = "Mã biên tập viên không hợp lệ.")
    private Integer reviewerId;
    @NotBlank(message = "Phải nhập lý do giao lại.")
    @Size(max = 1000, message = "Lý do không quá 1000 ký tự.")
    private String reason;
}
