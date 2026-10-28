package com.swp391.kpi.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO client gui len API. Dung o: AppealManagementController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   17  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter @Setter
public class AppealAssignRequest {
    @NotNull(message = "Phải chọn biên tập viên.")
    @Positive(message = "Mã biên tập viên không hợp lệ.")
    private Integer reviewerId;
    @Size(max = 1000, message = "Lý do không quá 1000 ký tự.")
    private String reason;
}
