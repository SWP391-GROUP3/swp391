package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO phan hoi bao cao tong hop su kien xu huong va tang dot bien (UC-65).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  field       ngay moc, cua so thoi gian, so su kien dot bien va danh sach su kien
 */
@Getter
@Setter
public class TrendingEventReportResponse {

    private LocalDate anchorDate;
    private LocalDate recentWindowStart;
    private LocalDate recentWindowEnd;
    private LocalDate baselineWindowStart;
    private LocalDate baselineWindowEnd;

    private Integer totalEvents;
    private Integer surgeCount;
    private List<TrendingEventItemResponse> items = new ArrayList<>();
}
