package com.swp391.kpi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppealCriterionRequest {

    @NotNull(message = "Tiêu chí khiếu nại không được để trống")
    private Integer criterionId;

    @NotBlank(message = "Lý do khiếu nại không được để trống")
    @Size(max = 1000, message = "Lý do khiếu nại không được vượt quá 1000 ký tự")
    private String reason;
}
