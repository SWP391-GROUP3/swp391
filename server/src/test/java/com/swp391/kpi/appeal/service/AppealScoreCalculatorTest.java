package com.swp391.kpi.appeal.service;

import com.swp391.kpi.appeal.entity.AppealResolutionRule;
import com.swp391.kpi.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AppealScoreCalculatorTest {
    private final AppealScoreCalculator calculator = new AppealScoreCalculator();
    private BigDecimal n(String value) { return new BigDecimal(value); }

    @Test void thresholdEqualityUsesS1ForBothDirections() {
        for (String score : List.of("65", "75")) {
            var result = calculator.compare(n("70"), n(score), null, n("5"));
            assertFalse(result.requiresSecondReview());
            assertEquals(n(score).setScale(2), result.effectiveScore());
            assertEquals(AppealResolutionRule.USE_S1, result.rule());
        }
    }

    @Test void bothIncreaseAndDecreaseBeyondThresholdRequireS2() {
        for (String score : List.of("64.99", "75.01")) {
            var result = calculator.compare(n("70"), n(score), null, n("5"));
            assertTrue(result.requiresSecondReview());
            assertNull(result.effectiveScore()); assertNull(result.rule());
        }
    }

    @Test void s2BelowBothUsesLowerOfS0AndS1() {
        var result = calculator.compare(n("50"), n("90"), n("40"), n("5"));
        assertEquals(n("50.00"), result.effectiveScore());
        assertEquals(AppealResolutionRule.MEDIAN_S0_S1_S2, result.rule());
    }

    @Test void s2AboveBothUsesHigherOfS0AndS1() {
        var result = calculator.compare(n("50"), n("90"), n("95"), n("5"));
        assertEquals(n("90.00"), result.effectiveScore());
        assertEquals(AppealResolutionRule.MEDIAN_S0_S1_S2, result.rule());
    }

    @Test void s2BetweenBothUsesS2() {
        var result = calculator.compare(n("50"), n("90"), n("89.99"), n("5"));
        assertEquals(n("89.99"), result.effectiveScore());
        assertEquals(AppealResolutionRule.MEDIAN_S0_S1_S2, result.rule());
    }

    @Test void medianHasNoJumpAroundMidpoint() {
        assertEquals(n("69.99"), calculator.compare(n("50"), n("90"), n("69.99"), n("5")).effectiveScore());
        assertEquals(n("70.01"), calculator.compare(n("50"), n("90"), n("70.01"), n("5")).effectiveScore());
    }

    @Test void zeroThresholdAndScoreBoundariesWork() {
        assertEquals(n("0.00"), calculator.compare(n("0"), n("0"), null, n("0")).effectiveScore());
        assertEquals(n("100.00"), calculator.compare(n("0"), n("100"), n("100"), n("0")).effectiveScore());
    }

    @Test void refusesExtraS2WhenNotRequiredAndInvalidComparisonInputs() {
        assertThrows(BusinessException.class, () -> calculator.compare(n("70"), n("72"), n("80"), n("5")));
        assertThrows(BusinessException.class, () -> calculator.compare(n("-1"), n("70"), null, n("5")));
        assertThrows(BusinessException.class, () -> calculator.compare(n("70"), n("101"), null, n("5")));
        assertThrows(BusinessException.class, () -> calculator.compare(n("70"), n("70"), null, n("-1")));
        assertThrows(BusinessException.class, () -> calculator.compare(null, n("70"), null, n("5")));
    }

    @Test void weightsArePercentAndTotalRoundsHalfUp() {
        var items = List.of(item(1,"33.33","49.99","Giải thích"), item(2,"66.67","80",null));
        assertEquals(n("70.00"), calculator.weightedTotal(items, true));
    }

    @Test void incompleteDraftIsAllowedButCannotBeSubmitted() {
        var items = List.of(item(1,"50","40",null), item(2,"50",null,null));
        assertNull(calculator.weightedTotal(items, false));
        assertThrows(BusinessException.class, () -> calculator.weightedTotal(items, true));
    }

    @Test void lowScoreRequiresCommentOnlyWhenSubmitting() {
        var items = List.of(item(1,"100","49.99"," "));
        assertEquals(n("49.99"), calculator.weightedTotal(items, false));
        assertThrows(BusinessException.class, () -> calculator.weightedTotal(items, true));
        assertEquals(n("50.00"), calculator.weightedTotal(List.of(item(1,"100","50",null)), true));
    }

    @Test void rejectsBadWeightsDuplicatesPrecisionAndScores() {
        for (var items : List.of(
                List.of(item(1,"99","70",null)),
                List.of(item(1,"50","70",null),item(1,"50","80",null)),
                List.of(item(1,"0","70",null),item(2,"100","80",null)),
                List.of(item(1,"100","100.01",null)),
                List.of(item(1,"100","70.001",null)))) {
            assertThrows(BusinessException.class, () -> calculator.weightedTotal(items, true));
        }
        assertThrows(BusinessException.class, () -> calculator.weightedTotal(List.of(), true));
    }

    private AppealScoreCalculator.Item item(int id, String weight, String score, String comment) {
        return new AppealScoreCalculator.Item(id, n(weight), score == null ? null : n(score), comment);
    }
}
