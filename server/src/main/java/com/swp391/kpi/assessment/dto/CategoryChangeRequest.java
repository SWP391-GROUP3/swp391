package com.swp391.kpi.assessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Request DTO cho BTV doi the loai bai viet truoc khi cham (UC-23, BR-SC-05)
/**
 * DTO client gui len API. Dung o: ScoringAttemptController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 *   24  CategoryChangeRequest()         ham khoi tao
 */
@Getter
@Setter
@NoArgsConstructor
public class CategoryChangeRequest {

    @NotNull(message = "Mã thể loại mới không được để trống")
    private Integer newCategoryId;

    public CategoryChangeRequest(Integer newCategoryId) {
        this.newCategoryId = newCategoryId;
    }
}
