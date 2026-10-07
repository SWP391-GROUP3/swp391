package com.swp391.kpi.royalty.dto;

import com.swp391.kpi.royalty.entity.RoyaltyPeriod;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RoyaltyPeriodResponse {

    private Integer id;
    private Integer periodYear;
    private Integer periodMonth;
    private String status;
    private LocalDateTime lockedAt;
    private String returnNote;
    private LocalDateTime paidAt;

    public static RoyaltyPeriodResponse from(RoyaltyPeriod period) {
        RoyaltyPeriodResponse dto = new RoyaltyPeriodResponse();
        dto.setId(period.getId());
        // Entity dung Short/Byte cho khop DB, API tra Integer cho de dung
        dto.setPeriodYear(period.getPeriodYear().intValue());
        dto.setPeriodMonth(period.getPeriodMonth().intValue());
        dto.setStatus(period.getStatus().name());
        dto.setLockedAt(period.getLockedAt());
        dto.setReturnNote(period.getReturnNote());
        dto.setPaidAt(period.getPaidAt());
        return dto;
    }
}
