package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO bieu dien thong tin phan tich tang dot bien cua 1 su kien (UC-65).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  field       cac truong thong tin su kien, luot xem 7 ngay, baseline 4 tuan, he so dot bien
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
