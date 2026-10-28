package com.swp391.kpi.dto;

import com.swp391.kpi.entity.ScoringDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO client gui len API. Dung o: ScoringAttemptController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoringSubmitRequest {

    @NotNull(message = "Quyết định đánh giá không được để trống")
    private ScoringDecision decision;

    @Size(max = 1000, message = "Ghi chú quyết định không được vượt quá 1000 ký tự")
    private String decisionNote;
}
