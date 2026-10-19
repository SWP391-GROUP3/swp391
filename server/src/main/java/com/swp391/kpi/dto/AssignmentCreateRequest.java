package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AssignmentCreateRequest {

    private Integer articleId;
    private Integer editorId;
    private Integer assignedBy;
    private Boolean wasSuggested = false;
}
