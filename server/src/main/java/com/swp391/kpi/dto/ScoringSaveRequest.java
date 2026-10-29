package com.swp391.kpi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO client gui len API. Dung o: ScoringAttemptController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   26  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoringSaveRequest {

    @NotEmpty(message = "Danh sách điểm tiêu chí không được để trống")
    @Valid
    private List<CriteriaScoreItemRequest> scores;
}
