package com.swp391.kpi.dto;

import com.swp391.kpi.entity.RoyaltyPeriod;
import com.swp391.kpi.entity.RoyaltyPeriodStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bang ke 1 ky nhuan but (UC-50 buoc 1): cac bai da cham + tong tien tung nguoi.
 * returnNote khac null = Ke toan da tra ky ve, Thu ky xem ly do roi sua (UC-50 A1).
 */
@Getter
@Setter
public class RoyaltyPeriodDetailResponse {

    private Integer id;
    private Short periodYear;
    private Byte periodMonth;
    private RoyaltyPeriodStatus status;
    private String returnNote;
    private LocalDateTime lockedAt;
    private LocalDateTime paidAt;
    private BigDecimal totalAmount;      // tong moi dong tien cua ky
    private Integer gradedArticleCount;  // so bai da cham trong ky

    private List<RoyaltyPeriodArticleResponse> articles;
    private List<RoyaltyPeriodPersonResponse> people;

    // Map phan thong tin ky; cac danh sach va tong tien service gan sau
    public static RoyaltyPeriodDetailResponse from(RoyaltyPeriod period) {
        RoyaltyPeriodDetailResponse response = new RoyaltyPeriodDetailResponse();
        response.setId(period.getId());
        response.setPeriodYear(period.getPeriodYear());
        response.setPeriodMonth(period.getPeriodMonth());
        response.setStatus(period.getStatus());
        response.setReturnNote(period.getReturnNote());
        response.setLockedAt(period.getLockedAt());
        response.setPaidAt(period.getPaidAt());
        return response;
    }
}
