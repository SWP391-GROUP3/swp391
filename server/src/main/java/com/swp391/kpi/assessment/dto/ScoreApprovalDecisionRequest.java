package com.swp391.kpi.assessment.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO client gui len API. Dung o: ScoreApprovalController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreApprovalDecisionRequest {

    @Size(max = 1000, message = "Lý do không được vượt quá 1000 ký tự")
    private String reason;
}
