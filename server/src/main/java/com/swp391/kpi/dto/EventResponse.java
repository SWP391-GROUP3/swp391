package com.swp391.kpi.dto;

import com.swp391.kpi.entity.Event;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 1 su kien tra ve cho o chon su kien cua BF-01 (BR-IN-03).
 * windowDays, articlesPerLevel de BF-01 dem hang cua bai; exemptScore de biet muc diem duoc mien giam (GBR-09).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  field                           cac truong tra ve cho giao dien
 *   32  from()                          Map tay entity -> DTO (AGENTS.md muc 3.4)
 */
@Getter
@Setter
public class EventResponse {

    private Integer id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer windowDays;       // cua so dem hang (ngay)
    private Integer articlesPerLevel; // = events.k_limit: so bai moi bac bao hoa (dat ten khac vi
                                      //   getter cua "kLimit" bi Jackson doi thanh "klimit")
    private BigDecimal exemptScore;   // diem >= muc nay thi mien giam

    // Map tay entity -> DTO (AGENTS.md muc 3.4)
    public static EventResponse from(Event event) {
        EventResponse response = new EventResponse();
        response.setId(event.getId());
        response.setName(event.getName());
        response.setStartDate(event.getStartDate());
        response.setEndDate(event.getEndDate());
        response.setWindowDays(event.getWindowDays());
        response.setArticlesPerLevel(event.getKLimit());
        response.setExemptScore(event.getExemptScore());
        return response;
    }
}
