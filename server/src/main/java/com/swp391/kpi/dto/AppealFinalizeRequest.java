package com.swp391.kpi.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AppealFinalizeRequest {
    @NotNull(message = "Thiếu mã phiếu dùng xác nhận kết quả")
    @Positive(message = "Mã phiếu phải là số dương")
    private Integer expectedAttemptId;
}
