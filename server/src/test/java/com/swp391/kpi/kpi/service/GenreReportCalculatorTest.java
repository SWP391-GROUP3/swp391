package com.swp391.kpi.kpi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GenreReportCalculator - Bộ tính toán báo cáo thể loại (UC-64, BR-KPI-04)")
class GenreReportCalculatorTest {

    private GenreReportCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new GenreReportCalculator();
    }

    @Test
    @DisplayName("Tính mean views chính xác cho danh sách lượt xem")
    void testCalculateMeanViews() {
        assertEquals(new BigDecimal("0.00"), calculator.calculateMeanViews(Collections.emptyList()));
        assertEquals(new BigDecimal("0.00"), calculator.calculateMeanViews(null));

        List<Long> views = Arrays.asList(1000L, 2000L, 3000L);
        // (1000 + 2000 + 3000) / 3 = 2000.00
        assertEquals(new BigDecimal("2000.00"), calculator.calculateMeanViews(views));

        List<Long> viewsWithZero = Arrays.asList(100L, 200L);
        assertEquals(new BigDecimal("150.00"), calculator.calculateMeanViews(viewsWithZero));
    }

    @Test
    @DisplayName("Tính median views chính xác cho cả số phần tử lẻ và chẵn")
    void testCalculateMedianViews() {
        assertEquals(new BigDecimal("0.00"), calculator.calculateMedianViews(Collections.emptyList()));

        // Số phần tử lẻ: [100, 300, 200] -> sorted [100, 200, 300] -> median = 200
        List<Long> oddViews = Arrays.asList(100L, 300L, 200L);
        assertEquals(new BigDecimal("200.00"), calculator.calculateMedianViews(oddViews));

        // Số phần tử chẵn: [100, 400, 200, 300] -> sorted [100, 200, 300, 400] -> median = (200 + 300)/2 = 250.00
        List<Long> evenViews = Arrays.asList(100L, 400L, 200L, 300L);
        assertEquals(new BigDecimal("250.00"), calculator.calculateMedianViews(evenViews));
    }

    @Test
    @DisplayName("Tính Bayesian Rating chính xác theo công thức có trọng số")
    void testCalculateBayesianRating() {
        // Chưa có lượt đánh giá nào -> tiệm cận về điểm trung bình toàn hệ thống (C = 3.5)
        BigDecimal resultNoRating = calculator.calculateBayesianRating(0, 0, 10, 3.5);
        assertEquals(new BigDecimal("3.50"), resultNoRating);

        // ratingCount = 10, ratingSum = 45 -> R = 4.5. m = 10, C = 3.5
        // WR = (10 / (10 + 10)) * 4.5 + (10 / (10 + 10)) * 3.5 = 0.5 * 4.5 + 0.5 * 3.5 = 2.25 + 1.75 = 4.00
        BigDecimal resultWeighted = calculator.calculateBayesianRating(10, 45, 10, 3.5);
        assertEquals(new BigDecimal("4.00"), resultWeighted);
    }

    @Test
    @DisplayName("Tính điểm chất lượng trung bình (Average Quality Score)")
    void testCalculateAverageQualityScore() {
        assertNull(calculator.calculateAverageQualityScore(null));
        assertNull(calculator.calculateAverageQualityScore(Collections.emptyList()));

        List<BigDecimal> scores = Arrays.asList(
                new BigDecimal("80.00"),
                new BigDecimal("90.00"),
                new BigDecimal("85.00")
        );
        assertEquals(new BigDecimal("85.00"), calculator.calculateAverageQualityScore(scores));
    }

    @Test
    @DisplayName("Tính chi phí nhuận bút / 1.000 lượt xem theo BR-KPI-04")
    void testCalculateCostPer1000Views() {
        // views = 0 -> trả về null (hiển thị N/A trên UI)
        assertNull(calculator.calculateCostPer1000Views(new BigDecimal("500000"), 0));
        assertNull(calculator.calculateCostPer1000Views(BigDecimal.ZERO, 10000));

        // Nhuận bút = 500.000đ, views = 10.000 -> Cost/1k = (500000 / 10000) * 1000 = 50.000,00
        BigDecimal cost = calculator.calculateCostPer1000Views(new BigDecimal("500000"), 10000);
        assertEquals(new BigDecimal("50000.00"), cost);
    }

    @Test
    @DisplayName("Kiểm tra cảnh báo thể loại ít bài hoặc thiếu dữ liệu")
    void testIsNeglectedOrInsufficient() {
        assertTrue(calculator.isNeglectedOrInsufficient(0, 3));
        assertTrue(calculator.isNeglectedOrInsufficient(2, 3));
        assertFalse(calculator.isNeglectedOrInsufficient(3, 3));
        assertFalse(calculator.isNeglectedOrInsufficient(5, 3));
    }
}
