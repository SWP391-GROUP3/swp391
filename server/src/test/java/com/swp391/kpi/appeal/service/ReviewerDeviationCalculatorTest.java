package com.swp391.kpi.appeal.service;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ReviewerDeviationCalculatorTest {
    private final ReviewerDeviationCalculator calculator = new ReviewerDeviationCalculator();

    @Test void averagesAbsoluteDifferencesAndRoundsHalfUp() {
        var result = calculator.calculate(List.of(new BigDecimal("-4.125"), new BigDecimal("7.125")), 3, 1, 8);
        assertEquals(new BigDecimal("5.63"), result.getAvgDeviation());
        assertEquals(new BigDecimal("33.33"), result.getOverturnRate());
        assertEquals(8, result.getWorkloadCount());
    }

    @Test void crossReviewWithoutAppealsHasZeroOverturnRate() {
        var result = calculator.calculate(List.of(new BigDecimal("-8.50")), 0, 0, 1);
        assertEquals(new BigDecimal("8.50"), result.getAvgDeviation());
        assertEquals(new BigDecimal("0.00"), result.getOverturnRate());
    }

    @Test void noComparisonMustNotCreateMisleadingZeroIndex() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(List.of(), 0, 0, 3));
    }
}
