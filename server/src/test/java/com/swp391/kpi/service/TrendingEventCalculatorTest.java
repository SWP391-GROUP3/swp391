package com.swp391.kpi.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TrendingEventCalculator - Bộ tính toán phát hiện sự kiện tăng đột biến (UC-65)")
class TrendingEventCalculatorTest {

    private TrendingEventCalculator calculator;
    private final LocalDate anchorDate = LocalDate.of(2026, 9, 20);

    @BeforeEach
    void setUp() {
        calculator = new TrendingEventCalculator();
    }

    @Test
    @DisplayName("Tính tổng lượt xem trong 7 ngày gần nhất chính xác")
    void testCalculateRecent7DaysViews() {
        assertEquals(0L, calculator.calculateRecent7DaysViews(Collections.emptyMap(), anchorDate));
        assertEquals(0L, calculator.calculateRecent7DaysViews(null, anchorDate));
        assertEquals(0L, calculator.calculateRecent7DaysViews(Collections.emptyMap(), null));

        Map<LocalDate, Long> viewsMap = new HashMap<>();
        // Trong cửa sổ 7 ngày [2026-09-14 đến 2026-09-20]
        viewsMap.put(LocalDate.of(2026, 9, 14), 100L);
        viewsMap.put(LocalDate.of(2026, 9, 15), 200L);
        viewsMap.put(LocalDate.of(2026, 9, 20), 300L);
        // Ngoài cửa sổ
        viewsMap.put(LocalDate.of(2026, 9, 13), 500L);
        viewsMap.put(LocalDate.of(2026, 9, 21), 1000L);

        // Tổng trong 7 ngày gần nhất = 100 + 200 + 300 = 600
        assertEquals(600L, calculator.calculateRecent7DaysViews(viewsMap, anchorDate));
    }

    @Test
    @DisplayName("Tính mức trung bình 7 ngày của 4 tuần cơ sở trước đó chính xác")
    void testCalculateBaseline4WeeksAverage() {
        assertEquals(new BigDecimal("0.00"), calculator.calculateBaseline4WeeksAverage(Collections.emptyMap(), anchorDate));

        Map<LocalDate, Long> viewsMap = new HashMap<>();
        // Cửa sổ 4 tuần trước: 28 ngày, từ [anchorDate - 34: 2026-08-17] đến [anchorDate - 7: 2026-09-13]
        // Tuần 1: 1000 views
        viewsMap.put(LocalDate.of(2026, 8, 20), 1000L);
        // Tuần 2: 2000 views
        viewsMap.put(LocalDate.of(2026, 8, 27), 2000L);
        // Tuần 3: 3000 views
        viewsMap.put(LocalDate.of(2026, 9, 3), 3000L);
        // Tuần 4: 2000 views
        viewsMap.put(LocalDate.of(2026, 9, 10), 2000L);

        // Tổng 28 ngày = 8000 views -> Trung bình mỗi tuần (7 ngày) = 8000 / 4 = 2000.00
        BigDecimal baseline = calculator.calculateBaseline4WeeksAverage(viewsMap, anchorDate);
        assertEquals(new BigDecimal("2000.00"), baseline);
    }

    @Test
    @DisplayName("Phát hiện tăng đột biến khi lượt xem 7 ngày vượt quá 2x mức trung bình 4 tuần cơ sở")
    void testIsSurgeDetected() {
        BigDecimal baseline = new BigDecimal("1000.00");

        // Dưới hoặc bằng 2x -> không tính là đột biến
        assertFalse(calculator.isSurgeDetected(1500L, baseline));
        assertFalse(calculator.isSurgeDetected(2000L, baseline));

        // Vượt quá 2x (vd: 2001, 3000) -> phát hiện đột biến (surge)
        assertTrue(calculator.isSurgeDetected(2001L, baseline));
        assertTrue(calculator.isSurgeDetected(3500L, baseline));

        // Baseline = 0 nhưng có view 7 ngày gần nhất -> đột biến mới
        assertTrue(calculator.isSurgeDetected(500L, BigDecimal.ZERO));
        assertFalse(calculator.isSurgeDetected(0L, BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Tính tỷ lệ tăng trưởng (surge ratio) chính xác")
    void testCalculateSurgeRatio() {
        BigDecimal baseline = new BigDecimal("1000.00");

        assertEquals(new BigDecimal("0.00"), calculator.calculateSurgeRatio(0L, baseline));
        assertEquals(new BigDecimal("1.50"), calculator.calculateSurgeRatio(1500L, baseline));
        assertEquals(new BigDecimal("2.50"), calculator.calculateSurgeRatio(2500L, baseline));
        assertEquals(new BigDecimal("999.00"), calculator.calculateSurgeRatio(100L, BigDecimal.ZERO));
    }
}
