package com.swp391.kpi.service;

import com.swp391.kpi.dto.CriteriaScoreInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Test cho Engine tính điểm chất lượng (ScoreCalculator)")
class ScoreCalculatorTest {

    private ScoreCalculator scoreCalculator;

    @BeforeEach
    void setUp() {
        scoreCalculator = new ScoreCalculator();
    }

    @Test
    @DisplayName("Tính tổng điểm chuẩn 4 tiêu chí mỗi tiêu chí 25% trọng số")
    void testCalculateTotalScore_EqualWeights() {
        List<CriteriaScoreInput> items = Arrays.asList(
                new CriteriaScoreInput(1, "Tính chính xác", new BigDecimal("25.00"), new BigDecimal("80.00"), "Tốt"),
                new CriteriaScoreInput(2, "Tính thời sự", new BigDecimal("25.00"), new BigDecimal("90.00"), "Rất nhanh"),
                new CriteriaScoreInput(3, "Văn phong", new BigDecimal("25.00"), new BigDecimal("70.00"), "Ổn"),
                new CriteriaScoreInput(4, "Đạo đức báo chí", new BigDecimal("25.00"), new BigDecimal("100.00"), "Chuẩn mực")
        );

        // Kỳ vọng: 80*0.25 + 90*0.25 + 70*0.25 + 100*0.25 = 20 + 22.5 + 17.5 + 25 = 85.00
        BigDecimal result = scoreCalculator.calculateTotalScore(items);
        assertEquals(new BigDecimal("85.00"), result);
    }

    @Test
    @DisplayName("Tính tổng điểm có làm tròn HALF_UP 2 chữ số thập phân")
    void testCalculateTotalScore_RoundingHalfUp() {
        List<CriteriaScoreInput> items = Arrays.asList(
                new CriteriaScoreInput(1, "Tiêu chí 1", new BigDecimal("33.33"), new BigDecimal("75.00"), "Đạt"),
                new CriteriaScoreInput(2, "Tiêu chí 2", new BigDecimal("33.33"), new BigDecimal("82.00"), "Đạt"),
                new CriteriaScoreInput(3, "Tiêu chí 3", new BigDecimal("33.34"), new BigDecimal("91.00"), "Đạt")
        );

        // 75 * 0.3333 = 24.9975
        // 82 * 0.3333 = 27.3306
        // 91 * 0.3334 = 30.3394
        // Tổng = 82.6675 -> HALF_UP = 82.67
        BigDecimal result = scoreCalculator.calculateTotalScore(items);
        assertEquals(new BigDecimal("82.67"), result);
    }

    @Test
    @DisplayName("Điểm < 50 bắt buộc phải có nhận xét (BR-SC-06) - Nếu thiếu thì ném ngoại lệ")
    void testValidate_ScoreUnder50_MissingComment() {
        List<CriteriaScoreInput> items = Collections.singletonList(
                new CriteriaScoreInput(1, "Văn phong", new BigDecimal("100.00"), new BigDecimal("45.00"), "   ")
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            scoreCalculator.calculateTotalScore(items);
        });

        assertTrue(ex.getMessage().contains("bắt buộc phải nhập nhận xét lý do"));
    }

    @Test
    @DisplayName("Điểm < 50 có nhận xét đầy đủ - Hợp lệ")
    void testValidate_ScoreUnder50_WithComment() {
        List<CriteriaScoreInput> items = Collections.singletonList(
                new CriteriaScoreInput(1, "Văn phong", new BigDecimal("100.00"), new BigDecimal("45.00"), "Sai nhiều lỗi chính tả")
        );

        BigDecimal result = scoreCalculator.calculateTotalScore(items);
        assertEquals(new BigDecimal("45.00"), result);
    }

    @Test
    @DisplayName("Điểm ngoài khoảng 0 - 100 ném ngoại lệ")
    void testValidate_ScoreOutOfRange() {
        List<CriteriaScoreInput> items = Collections.singletonList(
                new CriteriaScoreInput(1, "Tiêu chí", new BigDecimal("100.00"), new BigDecimal("105.00"), "Quá điểm")
        );

        assertThrows(IllegalArgumentException.class, () -> {
            scoreCalculator.calculateTotalScore(items);
        });
    }
}
