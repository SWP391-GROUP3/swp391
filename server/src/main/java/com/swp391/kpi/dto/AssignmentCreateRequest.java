package com.swp391.kpi.dto;

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

    private Integer articleId;
    private Integer editorId;
    private Boolean wasSuggested = false;
}
