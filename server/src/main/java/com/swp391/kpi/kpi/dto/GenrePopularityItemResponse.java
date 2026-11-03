package com.swp391.kpi.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO chi tiet cho tung the loai trong bao cao muc do quan tam the loai (UC-64).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class GenrePopularityItemResponse {

    private Integer genreId;
    private String genreCode;
    private String genreName;
    private String ndGroup;
    private BigDecimal baseFactor;

    private int articleCount;
    private long totalViews;
    private BigDecimal meanViews;
    private BigDecimal medianViews;

    private long ratingCount;
    private BigDecimal bayesianRating;

    private BigDecimal averageQualityScore;
    private BigDecimal totalRoyaltyPaid;
    private BigDecimal costPer1000Views; // null neu totalViews == 0 (UI hien "N/A" theo UC-64 E1)

    private boolean insufficientData; // true neu articleCount < 3 (UC-64 alert)
}
