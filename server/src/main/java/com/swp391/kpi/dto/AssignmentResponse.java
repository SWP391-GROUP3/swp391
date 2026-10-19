package com.swp391.kpi.dto;

import com.swp391.kpi.entity.Assignment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * MUC LUC (so dong)
 *   20  field                        - cac truong tra ve cho giao dien
 *   27  from()                       - doi entity sang DTO (map tay, AGENTS 3.4)
 */
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
