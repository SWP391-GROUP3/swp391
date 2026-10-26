package com.swp391.kpi.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreApprovalDecisionRequest {

    @Size(max = 1000, message = "Lý do không được vượt quá 1000 ký tự")
    private String reason;
}
