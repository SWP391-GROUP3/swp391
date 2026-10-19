package com.swp391.kpi.dto;

import com.swp391.kpi.entity.Assignment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class AssignmentResponse {

    private Integer id;
    private Integer articleId;
    private Integer editorId;
    private Integer assignedBy;
    private LocalDateTime assignedAt;
    private Boolean wasSuggested;

    public static AssignmentResponse from(Assignment entity) {
        if (entity == null) return null;
        AssignmentResponse dto = new AssignmentResponse();
        dto.setId(entity.getId());
        dto.setArticleId(entity.getArticleId());
        dto.setEditorId(entity.getEditorId());
        dto.setAssignedBy(entity.getAssignedBy());
        dto.setAssignedAt(entity.getAssignedAt());
        dto.setWasSuggested(entity.getWasSuggested());
        return dto;
    }
}
