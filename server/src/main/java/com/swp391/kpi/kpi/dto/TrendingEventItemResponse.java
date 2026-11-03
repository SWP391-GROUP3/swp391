package com.swp391.kpi.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO bieu dien thong tin phan tich tang dot bien cua 1 su kien (UC-65).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class TrendingEventItemResponse {

    private Integer eventId;
    private String eventName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;

    private Integer articleCount;
    private Long recent7DaysViews;
    private BigDecimal baselineWeeklyViews;
    private BigDecimal surgeRatio;
    private Boolean surgeDetected;
    private String statusLabel; // TĂNG ĐỘT BIẾN, BÌNH THƯỜNG, CHƯA ĐỦ DỮ LIỆU
}
