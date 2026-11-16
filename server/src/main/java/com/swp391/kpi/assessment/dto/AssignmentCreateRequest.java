package com.swp391.kpi.assessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO client gui len API. Dung o: AssignmentController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
@NoArgsConstructor
public class AssignmentCreateRequest {

    @NotNull(message = "Mã bài viết (articleId) không được để trống")
    private Integer articleId;

    @NotNull(message = "Mã Biên tập viên (editorId) không được để trống")
    private Integer editorId;

    private Boolean wasSuggested = false;
}
