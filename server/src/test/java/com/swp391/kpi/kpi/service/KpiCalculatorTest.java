package com.swp391.kpi.kpi.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("KpiCalculator - Logic tinh toan quy doi bai viet, diem TB va xet PASS/NOT_PASS")
class KpiCalculatorTest {

    private final KpiCalculator calculator = new KpiCalculator();

    @Test
    @DisplayName("calculateConvertedArticles: Tong hop dung ty le dong tac gia theo BR-KPI-02 va GBR-13")
    void testCalculateConvertedArticles() {
        // Phong vien A co 3 bai:
        // Bai 1: 100% (dung ten 1 minh) = 1.0 bai
        // Bai 2: 60% (dong tac gia chinh) = 0.6 bai
        // Bai 3: 40% (dong tac gia phu) = 0.4 bai
        // Tong quy doi: 1.0 + 0.6 + 0.4 = 2.0 bai -> int = 2
        List<KpiCalculator.ArticleKpiItem> items = List.of(
                new KpiCalculator.ArticleKpiItem(1, new BigDecimal("100.00"), new BigDecimal("85.00")),
                new KpiCalculator.ArticleKpiItem(2, new BigDecimal("60.00"), new BigDecimal("90.00")),
                new KpiCalculator.ArticleKpiItem(3, new BigDecimal("40.00"), new BigDecimal("75.00"))
        );

        int converted = calculator.calculateConvertedArticles(items);
        assertEquals(2, converted);

        BigDecimal exact = calculator.calculateExactConvertedArticles(items);
        assertEquals(new BigDecimal("2.00"), exact);
    }

    @Test
    @DisplayName("calculateAverageScore: Tinh trung binh diem chat luong bo qua cac bai chua co diem")
    void testCalculateAverageScore() {
        // Bai 1: 80.00
        // Bai 2: chua co diem (null)
        // Bai 3: 90.00
        // Diem TB = (80.00 + 90.00) / 2 = 85.00
        List<KpiCalculator.ArticleKpiItem> items = List.of(
                new KpiCalculator.ArticleKpiItem(1, new BigDecimal("100.00"), new BigDecimal("80.00")),
                new KpiCalculator.ArticleKpiItem(2, new BigDecimal("100.00"), null),
                new KpiCalculator.ArticleKpiItem(3, new BigDecimal("100.00"), new BigDecimal("90.00"))
        );

        BigDecimal avgScore = calculator.calculateAverageScore(items);
        assertNotNull(avgScore);
        assertEquals(new BigDecimal("85.00"), avgScore);
    }

    @Test
    @DisplayName("calculateAverageScore: Tra ve null khi danh sach trong hoac tat ca chua co diem")
    void testCalculateAverageScoreNull() {
        assertNull(calculator.calculateAverageScore(Collections.emptyList()));

        List<KpiCalculator.ArticleKpiItem> itemsWithoutScore = List.of(
                new KpiCalculator.ArticleKpiItem(1, new BigDecimal("100.00"), null)
        );
        assertNull(calculator.calculateAverageScore(itemsWithoutScore));
    }

    @Test
    @DisplayName("evaluatePassStatus: Danh gia binary PASS / NOT_PASS theo BR-KPI-05")
    void testEvaluatePassStatus() {
        int minPublished = 5;
        BigDecimal minAvgScore = new BigDecimal("70.00");

        // Dat ca 2 chi tieu -> PASS
        assertTrue(calculator.evaluatePassStatus(5, new BigDecimal("70.00"), minPublished, minAvgScore));
        assertTrue(calculator.evaluatePassStatus(6, new BigDecimal("85.50"), minPublished, minAvgScore));

        // Thieu so bai -> NOT_PASS
        assertFalse(calculator.evaluatePassStatus(4, new BigDecimal("95.00"), minPublished, minAvgScore));

        // Thieu diem trung binh -> NOT_PASS
        assertFalse(calculator.evaluatePassStatus(7, new BigDecimal("69.99"), minPublished, minAvgScore));

        // Chua co diem trung binh (null) -> NOT_PASS
        assertFalse(calculator.evaluatePassStatus(8, null, minPublished, minAvgScore));
    }
}
