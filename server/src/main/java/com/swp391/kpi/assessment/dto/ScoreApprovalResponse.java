package com.swp391.kpi.assessment.dto;

import com.swp391.kpi.assessment.entity.ApprovalDecision;
import com.swp391.kpi.assessment.entity.ScoreApproval;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO tra ve cho giao dien. Dung o: ScoreApprovalController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   25  field                           cac truong tra ve cho giao dien
 *   33  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreApprovalResponse {

    private Integer id;
    private Integer scoringAttemptId;
    private ApprovalDecision decision;
    private String reason;
    private Integer decidedBy;
    private LocalDateTime decidedAt;
    private LocalDateTime announcedAt;

    public static ScoreApprovalResponse from(ScoreApproval approval) {
        return new ScoreApprovalResponse(
                approval.getId(),
                approval.getScoringAttempt().getId(),
                approval.getDecision(),
                approval.getReason(),
                approval.getDecidedBy(),
                approval.getDecidedAt(),
                approval.getAnnouncedAt()
        );
    }
}
