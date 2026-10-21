package com.swp391.kpi.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AppealAssignRequest {
    @NotNull(message = "Phải chọn biên tập viên.")
    @Positive(message = "Mã biên tập viên không hợp lệ.")
    private Integer reviewerId;
    @Size(max = 1000, message = "Lý do không quá 1000 ký tự.")
    private String reason;
}
