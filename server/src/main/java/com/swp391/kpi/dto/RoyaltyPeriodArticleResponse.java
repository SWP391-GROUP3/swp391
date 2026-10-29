package com.swp391.kpi.dto;

import com.swp391.kpi.entity.RoyaltyGrading;
import com.swp391.kpi.entity.RoyaltyNature;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 1 bai da cham trong bang ke ky (UC-50): he so va tien cua bai.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  field                           cac truong tra ve cho giao dien
 *   34  from()                          title lay qua service BF-01; mediaAmount service cong tu cac dong MEDIA cua phieu
 */
@Getter
@Setter
public class RoyaltyPeriodArticleResponse {

    private Integer gradingId;
    private Integer articleId;
    private String title;
    private BigDecimal scoreUsed;
    private RoyaltyNature nature;
    private BigDecimal finalFactor;
    private BigDecimal articleAmount;   // tien phan bai viet (grading.amount)
    private BigDecimal mediaAmount;     // tong tien anh/video cua phieu trong ky
    private BigDecimal totalAmount;     // = articleAmount + mediaAmount
    private LocalDateTime gradedAt;

    // title lay qua service BF-01; mediaAmount service cong tu cac dong MEDIA cua phieu
    public static RoyaltyPeriodArticleResponse from(RoyaltyGrading grading, String title, BigDecimal mediaAmount) {
        RoyaltyPeriodArticleResponse response = new RoyaltyPeriodArticleResponse();
        response.setGradingId(grading.getId());
        response.setArticleId(grading.getArticleId());
        response.setTitle(title);
        response.setScoreUsed(grading.getScoreUsed());
        response.setNature(grading.getNature());
        response.setFinalFactor(grading.getFinalFactor());
        response.setArticleAmount(grading.getAmount());
        response.setMediaAmount(mediaAmount);
        response.setTotalAmount(grading.getAmount().add(mediaAmount));
        response.setGradedAt(grading.getGradedAt());
        return response;
    }
}
