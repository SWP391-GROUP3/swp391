package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MUC LUC (so dong)
 *   16  field                        - cac truong tra ve cho giao dien
 */
@Getter
@Setter
@NoArgsConstructor
public class AssignmentCreateRequest {

    private Integer articleId;
    private Integer editorId;
    private Integer assignedBy;
    private Boolean wasSuggested = false;
}
