package com.swp391.kpi.assessment.dto;

import com.swp391.kpi.assessment.entity.Assignment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO tra ve cho giao dien. Dung o: AssignmentController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   23  field                           cac truong tra ve cho giao dien
 *   36  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
@NoArgsConstructor
public class AssignmentResponse {

    private Integer id;
    private Integer articleId;
    private String articleTitle;
    private String articleStatus;
    private String categoryName;
    private Integer editorId;
    private Integer assignedBy;
    private LocalDateTime assignedAt;
    private Boolean wasSuggested;
    private String scoringStatus; // "CHUA_CHAM", "DANG_CHAM", "DA_CHAM"
    private BigDecimal totalScore;
    private String decision;

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
