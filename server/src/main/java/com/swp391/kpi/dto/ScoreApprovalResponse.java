package com.swp391.kpi.dto;

import com.swp391.kpi.entity.ApprovalDecision;
import com.swp391.kpi.entity.ScoreApproval;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

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
