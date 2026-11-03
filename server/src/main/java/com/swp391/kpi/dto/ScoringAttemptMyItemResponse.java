package com.swp391.kpi.dto;

import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.entity.AttemptType;
import com.swp391.kpi.entity.ScoringDecision;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 1 dong trong "Lich su cham" / trang chu cua BTV (M-A6, M-A7): bai duoc giao + lan cham BF-02 moi nhat.
 * Dung o: ScoringAttemptService.getMyScorings(), ScoringAttemptController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   25  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
@NoArgsConstructor
public class ScoringAttemptMyItemResponse {

    private Integer articleId;
    private String title;
    private ArticleStatus articleStatus;
    private String categoryName;
    private LocalDateTime assignedAt;

    // Lan cham moi nhat (khong tinh lan APPEAL cua BF-03); chua mo phieu thi null
    private Integer attemptId;
    private Integer attemptNo;
    private AttemptType attemptType;
    private BigDecimal totalScore;
    private ScoringDecision decision;
    private LocalDateTime submittedAt;

    // UC-32: Truong ban tra cham lai -> ly do va thoi diem tra (lay tu lan cham bi thay the)
    private String returnReason;
    private LocalDateTime returnedAt;
}
