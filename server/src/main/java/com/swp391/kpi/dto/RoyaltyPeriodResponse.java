package com.swp391.kpi.dto;

import com.swp391.kpi.entity.RoyaltyPeriod;
import com.swp391.kpi.entity.RoyaltyPeriodStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 1 ky nhuan but tra ve cho man Royalty Period List (UC-49).
 */
@Getter
@Setter
public class RoyaltyPeriodResponse {

    private Integer id;
    private Short periodYear;
    private Byte periodMonth;
    private RoyaltyPeriodStatus status;
    private LocalDateTime lockedAt;
    private String returnNote;
    private LocalDateTime paidAt;
    private BigDecimal totalAmount;      // tong tien cac dong trong ky
    private Integer gradedArticleCount;  // so bai da cham trong ky

    // Map tay entity -> DTO (AGENTS.md muc 3.4), them 2 so lieu tong hop tinh tu bang khac
    public static RoyaltyPeriodResponse from(RoyaltyPeriod period, BigDecimal totalAmount,
                                             Integer gradedArticleCount) {
        RoyaltyPeriodResponse response = new RoyaltyPeriodResponse();
        response.setId(period.getId());
        response.setPeriodYear(period.getPeriodYear());
        response.setPeriodMonth(period.getPeriodMonth());
        response.setStatus(period.getStatus());
        response.setLockedAt(period.getLockedAt());
        response.setReturnNote(period.getReturnNote());
        response.setPaidAt(period.getPaidAt());
        response.setTotalAmount(totalAmount);
        response.setGradedArticleCount(gradedArticleCount);
        return response;
    }
}
