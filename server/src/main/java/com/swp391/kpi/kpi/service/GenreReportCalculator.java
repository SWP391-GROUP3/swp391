package com.swp391.kpi.kpi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bo tinh toan thuan cho bao cao muc do quan tam the loai & chi phi nhuan but (UC-64, BR-KPI-04).
 * Khong doc DB, khong co @Service (AGENTS.md muc 1, 3.1, 3.9).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   33  calculateMeanViews()            Tinh luot xem trung binh (mean views) tren danh sach luot xem cua cac bai viet
 *   53  calculateMedianViews()          Tinh luot xem trung vi (median views) tren danh sach luot xem cua cac bai viet
 *   84  calculateBayesianRating()       Tinh diem danh gia Bayesian Rating (WR - Weighted Rating): WR = (v / (v + m)) * R + (m / (v + m)) * C Trong d…
 *  109  calculateAverageQualityScore()  Tinh diem chat luong trung binh cua cac bai viet trong the loai (Average Quality Score)
 *  136  calculateCostPer1000Views()     Tinh chi phi nhuan but tren 1.000 luot xem (BR-KPI-04): Royalty Cost per 1,000 Views = (Total Royalty Paid / …
 *  151  isNeglectedOrInsufficient()     Kiem tra xem the loai co it bai viet hoac co nguy co bi lang quen khong (neglected genre / insufficient data)
 */
public class GenreReportCalculator {

    public static final int DEFAULT_DECIMAL_SCALE = 2;
    public static final long DEFAULT_BAYESIAN_M = 10L; // Nguong toi thieu danh gia m
    public static final double DEFAULT_GLOBAL_RATING_C = 3.5; // Diem danh gia trung binh toan he thong mac dinh
    public static final int DEFAULT_MIN_ARTICLES_K = 3; // Nguong canh bao it bai viet / can chu y

    /**
     * Tinh luot xem trung binh (mean views) tren danh sach luot xem cua cac bai viet.
     * Neu danh sach rong hoac null tra ve 0.
     */
    public BigDecimal calculateMeanViews(List<Long> viewsList) {
        if (viewsList == null || viewsList.isEmpty()) {
            return BigDecimal.ZERO.setScale(DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
        }

        long totalViews = 0;
        for (Long v : viewsList) {
            if (v != null && v > 0) {
                totalViews += v;
            }
        }

        return BigDecimal.valueOf(totalViews)
                .divide(BigDecimal.valueOf(viewsList.size()), DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Tinh luot xem trung vi (median views) tren danh sach luot xem cua cac bai viet.
     * Sap xep tang dan va lay phan tu o giua.
     */
    public BigDecimal calculateMedianViews(List<Long> viewsList) {
        if (viewsList == null || viewsList.isEmpty()) {
            return BigDecimal.ZERO.setScale(DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
        }

        List<Long> sorted = new ArrayList<>();
        for (Long v : viewsList) {
            sorted.add(v == null ? 0L : v);
        }
        Collections.sort(sorted);

        int size = sorted.size();
        if (size % 2 == 1) {
            return BigDecimal.valueOf(sorted.get(size / 2)).setScale(DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
        } else {
            long mid1 = sorted.get((size / 2) - 1);
            long mid2 = sorted.get(size / 2);
            return BigDecimal.valueOf(mid1 + mid2)
                    .divide(BigDecimal.valueOf(2), DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
        }
    }

    /**
     * Tinh diem danh gia Bayesian Rating (WR - Weighted Rating):
     * WR = (v / (v + m)) * R + (m / (v + m)) * C
     * Trong do:
     *   v: Tong so luot danh gia cua the loai (rating_count)
     *   m: Tham so nguong trong so (m)
     *   R: Diem trung binh thuc te cua the loai (rating_sum / rating_count)
     *   C: Diem trung binh toan he thong (global average rating)
     */
    public BigDecimal calculateBayesianRating(long ratingCount, long ratingSum, long m, double globalC) {
        if (m <= 0) {
            m = DEFAULT_BAYESIAN_M;
        }

        double c = (globalC > 0) ? globalC : DEFAULT_GLOBAL_RATING_C;

        if (ratingCount <= 0 || ratingSum <= 0) {
            // Chua co luot danh gia nao, Bayesian rating tien ve C
            return BigDecimal.valueOf(c).setScale(DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
        }

        double r = (double) ratingSum / ratingCount;
        double v = (double) ratingCount;

        // WR = (v / (v + m)) * R + (m / (v + m)) * C
        double bayesian = (v / (v + m)) * r + (m / (v + m)) * c;

        return BigDecimal.valueOf(bayesian).setScale(DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Tinh diem chat luong trung binh cua cac bai viet trong the loai (Average Quality Score).
     * Chi tinh cac bai da co diem chat luong (> 0). Neu khong co bai nao thi tra ve null.
     */
    public BigDecimal calculateAverageQualityScore(List<BigDecimal> qualityScores) {
        if (qualityScores == null || qualityScores.isEmpty()) {
            return null;
        }

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;

        for (BigDecimal s : qualityScores) {
            if (s != null) {
                sum = sum.add(s);
                count++;
            }
        }

        if (count == 0) {
            return null;
        }

        return sum.divide(BigDecimal.valueOf(count), DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Tinh chi phi nhuan but tren 1.000 luot xem (BR-KPI-04):
     * Royalty Cost per 1,000 Views = (Total Royalty Paid / Total Page Views) * 1,000
     * Neu tong luot xem bang 0 hoac chua co du lieu doc gia, tra ve null (UI hien "N/A" theo UC-64 E1).
     */
    public BigDecimal calculateCostPer1000Views(BigDecimal totalRoyaltyPaid, long totalPageViews) {
        if (totalPageViews <= 0 || totalRoyaltyPaid == null || totalRoyaltyPaid.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // Tinh (totalRoyaltyPaid * 1000) / totalPageViews
        BigDecimal numerator = totalRoyaltyPaid.multiply(BigDecimal.valueOf(1000));
        return numerator.divide(BigDecimal.valueOf(totalPageViews), DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Kiem tra xem the loai co it bai viet hoac co nguy co bi lang quen khong (neglected genre / insufficient data).
     * @param articleCount So luong bai viet xuat ban cua the loai trong ky
     * @param minThreshold Nguong toi thieu k (mac dinh 3)
     */
    public boolean isNeglectedOrInsufficient(int articleCount, int minThreshold) {
        int threshold = (minThreshold > 0) ? minThreshold : DEFAULT_MIN_ARTICLES_K;
        return articleCount < threshold;
    }
}
