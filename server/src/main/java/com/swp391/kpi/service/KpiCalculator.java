package com.swp391.kpi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * May tinh toan KPI (BF-05): quy doi so bai theo ty le dong tac gia,
 * tinh diem trung binh chat luong va xet dat/khong dat (PASS/NOT_PASS).
 * Class tinh toan thuan: khong doc DB, khong co @Service (AGENTS.md muc 3.1, 3.9).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   28  ArticleKpiItem()                DTO chua thong tin 1 bai viet cua phong vien trong ky phuc vu tinh KPI
 *   39  calculateConvertedArticles()    Tinh tong so bai viet quy doi (BR-KPI-02 & GBR-13)
 *   57  calculateExactConvertedArticles()  Tinh tong so bai viet quy doi dang so thap phan chinh xac (dung cho hien thi Dashboard)
 *   77  calculateAverageScore()         Tinh diem trung binh chat luong cua phong vien trong ky (BR-KPI-02)
 *  104  evaluatePassStatus()            Xac dinh ket qua KPI theo quy tac nhi phan PASS / NOT_PASS (BR-KPI-05)
 */
public class KpiCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final int SCORE_SCALE = 2;

    /**
     * DTO chua thong tin 1 bai viet cua phong vien trong ky phuc vu tinh KPI.
     */
    public record ArticleKpiItem(
            Integer articleId,
            BigDecimal sharePercent, // Ty le dong tac gia (%) vi du: 100.00, 60.00, 40.00
            BigDecimal effectiveScore // Diem chat luong bai viet (thang 0-100), co the null neu chua co diem
    ) {}

    /**
     * Tinh tong so bai viet quy doi (BR-KPI-02 & GBR-13).
     * Moi bai quy doi = sharePercent / 100 (vd: 60% = 0.6 bai, 40% = 0.4 bai).
     * Lam tron thanh so nguyen (INT theo schema.sql actual_articles) bang HALF_UP.
     */
    public int calculateConvertedArticles(List<ArticleKpiItem> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (ArticleKpiItem item : items) {
            if (item != null && item.sharePercent() != null) {
                BigDecimal share = item.sharePercent().divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP);
                total = total.add(share);
            }
        }
        return total.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * Tinh tong so bai viet quy doi dang so thap phan chinh xac (dung cho hien thi Dashboard).
     */
    public BigDecimal calculateExactConvertedArticles(List<ArticleKpiItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (ArticleKpiItem item : items) {
            if (item != null && item.sharePercent() != null) {
                BigDecimal share = item.sharePercent().divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
                total = total.add(share);
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Tinh diem trung binh chat luong cua phong vien trong ky (BR-KPI-02).
     * Chi tinh tren cac bai viet da co diem (effectiveScore != null).
     * Neu chua co bai nao co diem thi tra ve null (khop actual_avg_score trong schema.sql).
     */
    public BigDecimal calculateAverageScore(List<ArticleKpiItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        int scoredCount = 0;

        for (ArticleKpiItem item : items) {
            if (item != null && item.effectiveScore() != null) {
                totalScore = totalScore.add(item.effectiveScore());
                scoredCount++;
            }
        }

        if (scoredCount == 0) {
            return null;
        }

        return totalScore.divide(BigDecimal.valueOf(scoredCount), SCORE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Xac dinh ket qua KPI theo quy tac nhi phan PASS / NOT_PASS (BR-KPI-05).
     * PASS khi: actualArticles >= minPublished VA actualAvgScore >= minAvgScore.
     * Nguoc lai la NOT_PASS. Cam dung xep hang A/B/C/D.
     */
    public boolean evaluatePassStatus(int actualArticles, BigDecimal actualAvgScore,
                                      int minPublished, BigDecimal minAvgScore) {
        if (actualArticles < minPublished) {
            return false;
        }
        if (actualAvgScore == null) {
            return false;
        }
        return actualAvgScore.compareTo(minAvgScore) >= 0;
    }
}
