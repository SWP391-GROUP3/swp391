package com.swp391.kpi.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO tong the tra ve cho bao cao muc do quan tam the loai & chi phi nhuan but (UC-64).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  field                           cac truong tong hop toan bo the loai
 */
@Getter
@Setter
public class GenrePopularityReportResponse {

    private Short year;
    private Byte month; // co the null neu xem toan bo hoac chua chon thang
    private int totalGenres;
    private int totalArticles;
    private long totalViews;
    private BigDecimal totalRoyaltyPaid;
    private BigDecimal globalMeanViews;
    private BigDecimal globalBayesianRating;

    private List<GenrePopularityItemResponse> items;
}
