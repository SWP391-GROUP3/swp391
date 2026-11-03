package com.swp391.kpi.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO tra ve cho giao dien. Dung o: AssignmentController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   20  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EditorWorkloadDTO {

    private Integer editorId;
    private long activeArticleCount;
}
