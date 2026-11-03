package com.swp391.kpi.royalty.service;

import com.swp391.kpi.royalty.entity.RoyaltyNature;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Kiem tra RoyaltyCalculator (BF-04). Class tinh toan thuan nen test chay
 * khong can Spring, khong can DB.
 */
class RoyaltyCalculatorTest {

    // Tham so BF-04 giong het bang system_parameters trong database/data.sql
    private static Map<String, String> defaultParams() {
        Map<String, String> params = new HashMap<>();
        params.put("NATURE_RATE_ORIGINAL", "100");
        params.put("NATURE_RATE_TRANSLATION", "70");
        params.put("NATURE_RATE_COMPILATION", "50");
        params.put("NATURE_RATE_REPOST", "30");
        params.put("MIN_FINAL_FACTOR", "1");
        params.put("ROUNDING_UNIT", "1000");
        params.put("UNIT_PRICE_PERCENT", "10");   // chua co trong data.sql: cho PR [DB]
        return params;
    }

    // So sanh BigDecimal theo GIA TRI (100 va 100.00 coi la bang nhau)
    private static void assertSameValue(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "Mong doi " + expected + " nhung ra " + actual);
    }

    @Test
    void createsCalculatorWhenAllParametersArePresent() {
        assertDoesNotThrow(() -> new RoyaltyCalculator(defaultParams()));
    }

    @Test
    void throwsWhenAParameterIsMissing() {
        Map<String, String> params = defaultParams();
        params.remove("ROUNDING_UNIT");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new RoyaltyCalculator(params));

        assertEquals("Thiếu tham số hệ thống: ROUNDING_UNIT", ex.getMessage());
    }

    @Test
    void throwsWhenAParameterIsNotANumber() {
        Map<String, String> params = defaultParams();
        params.put("NATURE_RATE_ORIGINAL", "một trăm");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new RoyaltyCalculator(params));

        assertEquals("Tham số hệ thống NATURE_RATE_ORIGINAL không phải là số: một trăm", ex.getMessage());
    }

    @Test
    void suggestedFactorIsBaseFactorTimesScorePercent() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());
        BigDecimal reportageBase = new BigDecimal("5.00");     // Bai phan anh trong data.sql

        assertSameValue("5.00", calculator.suggestedFactor(reportageBase, new BigDecimal("100")));  // 5.00 x 100%
        assertSameValue("4.25", calculator.suggestedFactor(reportageBase, new BigDecimal("85")));   // 5.00 x 85%
        assertSameValue("3.85", calculator.suggestedFactor(reportageBase, new BigDecimal("77")));   // bai 1 trong data.sql
    }

    @Test
    void suggestedFactorIsRoundedToTwoDecimals() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());
        BigDecimal score = new BigDecimal("85");

        assertSameValue("0.86", calculator.suggestedFactor(new BigDecimal("1.01"), score));  // 0.8585 -> 0.86
        assertSameValue("2.01", calculator.suggestedFactor(new BigDecimal("2.37"), score));  // 2.0145 -> 2.01
    }

    @Test
    void articleNotInEventGetsFullSaturationRate() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());

        // Khong thuoc su kien: articles.saturation_rate = NULL, khong co exempt_score
        assertSameValue("100", calculator.effectiveSaturationRate(new BigDecimal("75"), null, null));
    }

    @Test
    void scoreBelowExemptScoreKeepsArticleSaturationRate() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());
        BigDecimal exemptScore = new BigDecimal("90");

        assertSameValue("80", calculator.effectiveSaturationRate(new BigDecimal("85"), new BigDecimal("80"), exemptScore));
        assertSameValue("60", calculator.effectiveSaturationRate(new BigDecimal("89.99"), new BigDecimal("60"), exemptScore));
    }

    @Test
    void scoreAtOrAboveExemptScoreIsExempt() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());
        BigDecimal exemptScore = new BigDecimal("90");

        assertSameValue("100", calculator.effectiveSaturationRate(new BigDecimal("90"), new BigDecimal("60"), exemptScore));  // dung nguong
        assertSameValue("100", calculator.effectiveSaturationRate(new BigDecimal("95"), new BigDecimal("80"), exemptScore));
    }

    @Test
    void natureRateComesFromSystemParameters() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());

        assertSameValue("100", calculator.natureRate(RoyaltyNature.ORIGINAL));
        assertSameValue("70", calculator.natureRate(RoyaltyNature.TRANSLATION));
        assertSameValue("50", calculator.natureRate(RoyaltyNature.COMPILATION));
        assertSameValue("30", calculator.natureRate(RoyaltyNature.REPOST));
    }

    @Test
    void natureRateFollowsChangedParameter() {
        Map<String, String> params = defaultParams();
        params.put("NATURE_RATE_TRANSLATION", "65");   // Admin doi ty le bai dich trong system_parameters

        RoyaltyCalculator calculator = new RoyaltyCalculator(params);

        assertSameValue("65", calculator.natureRate(RoyaltyNature.TRANSLATION));
    }

    @Test
    void finalFactorMultipliesSaturationAndNatureRates() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());

        // Bai goc, khong bao hoa: giu nguyen he so
        assertSameValue("3.85", calculator.finalFactor(new BigDecimal("3.85"), new BigDecimal("100"), RoyaltyNature.ORIGINAL));
        // 5.00 x 80% x 70% = 2.80
        assertSameValue("2.80", calculator.finalFactor(new BigDecimal("5.00"), new BigDecimal("80"), RoyaltyNature.TRANSLATION));
    }

    @Test
    void finalFactorIsRoundedOnlyOnceAtTheEnd() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());

        // 3.85 x 80% x 70% = 2.156 -> 2.16
        assertSameValue("2.16", calculator.finalFactor(new BigDecimal("3.85"), new BigDecimal("80"), RoyaltyNature.TRANSLATION));
        // 4.06 x 80% x 70% = 2.2736 -> 2.27
        // (neu lam tron giua chung: 4.06 x 80% = 3.248 -> 3.25, roi 3.25 x 70% = 2.275 -> 2.28: SAI)
        assertSameValue("2.27", calculator.finalFactor(new BigDecimal("4.06"), new BigDecimal("80"), RoyaltyNature.TRANSLATION));
    }

    @Test
    void finalFactorIsAtLeastMinFinalFactor() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());

        // 3.85 x 60% x 30% = 0.693 -> nang len toi thieu 1
        assertSameValue("1", calculator.finalFactor(new BigDecimal("3.85"), new BigDecimal("60"), RoyaltyNature.REPOST));
        // 2.00 x 100% x 50% = 1.00: dung bang toi thieu thi giu nguyen
        assertSameValue("1.00", calculator.finalFactor(new BigDecimal("2.00"), new BigDecimal("100"), RoyaltyNature.COMPILATION));
    }

    // ---------- Don gia 1 he so tu luong co so (UC-55) ----------

    @Test
    void unitPriceIsBaseSalaryTimesPercent() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());

        // 2.340.000 x 10% = 234.000 (dung don gia trong data.sql)
        assertSameValue("234000", calculator.unitPrice(new BigDecimal("2340000")));
        // 2.345.678 x 10% = 234.567,8 -> bo phan le duoi 1 dong -> 234.567
        assertSameValue("234567", calculator.unitPrice(new BigDecimal("2345678")));
    }

    // ---------- Buoc 6: tien = he so x don gia, lam tron XUONG 1.000d ----------

    @Test
    void amountIsFactorTimesUnitPriceRoundedDownToThousand() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());
        BigDecimal unitPrice = new BigDecimal("234000");   // don gia trong data.sql (10% luong co so)

        // 5.27 x 234.000 = 1.233.180 -> 1.233.000
        assertSameValue("1233000", calculator.calculateAmount(new BigDecimal("5.27"), unitPrice));
        // 1.99 x 234.000 = 465.660 -> 465.000 (lam tron GAN NHAT se ra 466.000: SAI)
        assertSameValue("465000", calculator.calculateAmount(new BigDecimal("1.99"), unitPrice));
        // 5.00 x 234.000 = 1.170.000: da chan nghin thi giu nguyen
        assertSameValue("1170000", calculator.calculateAmount(new BigDecimal("5.00"), unitPrice));
    }

    // ---------- Buoc 7: chia tien cho cac tac gia (GBR-10) ----------

    // Danh sach % cua tung tac gia, phan tu dau tien la tac gia chinh
    private static List<BigDecimal> shares(String... percents) {
        List<BigDecimal> result = new ArrayList<>();
        for (String percent : percents) {
            result.add(new BigDecimal(percent));
        }
        return result;
    }

    @Test
    void coAuthorsAreRoundedDownAndMainAuthorTakesTheRest() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());

        // 1.170.000 chia 40 / 35 / 25 (tac gia chinh 40%)
        //   dong tac gia 35%: 409.500 -> 409.000
        //   dong tac gia 25%: 292.500 -> 292.000
        //   tac gia chinh   : 1.170.000 - 409.000 - 292.000 = 469.000 (khong phai 468.000)
        List<BigDecimal> parts = calculator.splitAmount(new BigDecimal("1170000"), shares("40", "35", "25"));

        assertEquals(3, parts.size());
        assertSameValue("469000", parts.get(0));
        assertSameValue("409000", parts.get(1));
        assertSameValue("292000", parts.get(2));
    }

    @Test
    void splitPartsAlwaysAddUpToTheWholeAmount() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());

        // 33.33% cua 1.233.000 = 410.958,9 -> 410.000 moi dong tac gia; tac gia chinh 413.000
        List<BigDecimal> parts = calculator.splitAmount(new BigDecimal("1233000"), shares("33.34", "33.33", "33.33"));

        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal part : parts) {
            total = total.add(part);
        }
        assertSameValue("1233000", total);   // khong mat dong nao
        assertSameValue("413000", parts.get(0));
    }

    @Test
    void singleAuthorGetsTheWholeAmount() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());

        List<BigDecimal> parts = calculator.splitAmount(new BigDecimal("1233000"), shares("100"));

        assertEquals(1, parts.size());
        assertSameValue("1233000", parts.get(0));
    }

    @Test
    void throwsWhenSharesDoNotAddUpTo100() {
        RoyaltyCalculator calculator = new RoyaltyCalculator(defaultParams());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> calculator.splitAmount(new BigDecimal("1170000"), shares("60", "30")));

        assertEquals("Tổng tỷ lệ chia của các tác giả phải bằng 100%, đang là 90", ex.getMessage());
    }

    // ---------------------------------------------------- bao hoa theo hang (GBR-09)

    private static final BigDecimal LEVEL_2 = new BigDecimal("80");  // giong SATURATION_RATE_LEVEL_2 trong data.sql
    private static final BigDecimal LEVEL_3 = new BigDecimal("60");  // giong SATURATION_RATE_LEVEL_3

    @Test
    void saturationRateIsFullForFirstKLimitArticles() {
        assertSameValue("100", RoyaltyCalculator.saturationRateByRank(1, 10, LEVEL_2, LEVEL_3));
        assertSameValue("100", RoyaltyCalculator.saturationRateByRank(10, 10, LEVEL_2, LEVEL_3));
    }

    @Test
    void saturationRateDropsToLevel2FromRankKPlus1To2K() {
        assertSameValue("80", RoyaltyCalculator.saturationRateByRank(11, 10, LEVEL_2, LEVEL_3));
        assertSameValue("80", RoyaltyCalculator.saturationRateByRank(20, 10, LEVEL_2, LEVEL_3));
    }

    @Test
    void saturationRateDropsToLevel3FromRank2KPlus1() {
        assertSameValue("60", RoyaltyCalculator.saturationRateByRank(21, 10, LEVEL_2, LEVEL_3));
        assertSameValue("60", RoyaltyCalculator.saturationRateByRank(100, 10, LEVEL_2, LEVEL_3));
    }

    @Test
    void saturationRateThrowsWhenRankIsBelowOne() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> RoyaltyCalculator.saturationRateByRank(0, 10, LEVEL_2, LEVEL_3));

        assertEquals("Hạng của bài và k_limit của sự kiện phải từ 1 trở lên.", ex.getMessage());
    }
}
