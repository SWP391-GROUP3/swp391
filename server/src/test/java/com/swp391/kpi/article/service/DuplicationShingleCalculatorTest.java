package com.swp391.kpi.article.service;

import com.swp391.kpi.article.entity.DuplicationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra DuplicationShingleCalculator (UC-06, BR-IN-04, B1-B4). Class tinh thuan nen test chay
 * khong can Spring, khong can DB.
 */
class DuplicationShingleCalculatorTest {

    private static final int SIZE = 5;   // SHINGLE_SIZE mac dinh trong data.sql

    private static Set<String> shingleSet(String text) {
        return new HashSet<>(DuplicationShingleCalculator.shingles(DuplicationShingleCalculator.splitWords(text), SIZE));
    }

    @Test
    void normalize_lowercaseRemovePunctuationKeepVietnameseMarks() {
        assertEquals("việt", DuplicationShingleCalculator.normalize("Việt,"));
        assertEquals("nam", DuplicationShingleCalculator.normalize("\"NAM!\""));
        assertEquals("", DuplicationShingleCalculator.normalize("..."));
    }

    @Test
    void splitWords_skipPunctuationOnlyTokens() {
        List<String> words = DuplicationShingleCalculator.splitWords("  Hà Nội  -  mùa thu ... ");
        assertEquals(List.of("Hà", "Nội", "mùa", "thu"), words);
    }

    @Test
    void shingles_fewerWordsThanSize_isEmpty() {
        List<String> words = DuplicationShingleCalculator.splitWords("một hai ba bốn");
        assertTrue(DuplicationShingleCalculator.shingles(words, SIZE).isEmpty());
    }

    @Test
    void overlapPercent_sameTextDifferentCaseAndPunctuation_is100() {
        List<String> mine = DuplicationShingleCalculator.shingles(
                DuplicationShingleCalculator.splitWords("Hôm nay trời đẹp, chúng tôi đi chơi công viên."), SIZE);
        Set<String> other = shingleSet("hôm nay trời đẹp chúng tôi đi chơi công viên");
        assertEquals(new BigDecimal("100.00"), DuplicationShingleCalculator.overlapPercent(mine, other));
    }

    @Test
    void overlapPercent_partOfText() {
        // 10 am tiet -> 6 cum; 5 am tiet dau trung -> chi cum dau trung -> 1/6 = 16.67%
        List<String> mine = DuplicationShingleCalculator.shingles(
                DuplicationShingleCalculator.splitWords("a b c d e f g h i k"), SIZE);
        Set<String> other = shingleSet("a b c d e x y z");
        assertEquals(new BigDecimal("16.67"), DuplicationShingleCalculator.overlapPercent(mine, other));
    }

    @Test
    void overlapPercent_noShingles_isZero() {
        assertEquals(new BigDecimal("0.00"), DuplicationShingleCalculator.overlapPercent(List.of(), Set.of("a b c d e")));
    }

    @Test
    void passages_mergeAdjacentMatchesAndKeepOriginalText() {
        List<String> words = DuplicationShingleCalculator.splitWords("Một hai ba bốn năm, X Y sáu bảy tám chín mười.");
        List<String> mine = DuplicationShingleCalculator.shingles(words, SIZE);
        Set<String> other = shingleSet("một hai ba bốn năm khác sáu bảy tám chín mười");
        List<String> passages = DuplicationShingleCalculator.passages(words, mine, other, SIZE);
        assertEquals(List.of("Một hai ba bốn năm,", "sáu bảy tám chín mười."), passages);
    }

    @Test
    void classify_thresholds() {
        assertEquals(DuplicationResult.NORMAL, DuplicationShingleCalculator.classify(new BigDecimal("19.99"), 20, 50, true));
        assertEquals(DuplicationResult.NEED_EXPLAIN, DuplicationShingleCalculator.classify(new BigDecimal("20.00"), 20, 50, true));
        assertEquals(DuplicationResult.NEED_EXPLAIN, DuplicationShingleCalculator.classify(new BigDecimal("50.00"), 20, 50, true));
        assertEquals(DuplicationResult.BLOCKED, DuplicationShingleCalculator.classify(new BigDecimal("50.01"), 20, 50, true));
        // Tren 50% nhung khong phai Sang tac -> chi can giai trinh (UC-05 A1)
        assertEquals(DuplicationResult.NEED_EXPLAIN, DuplicationShingleCalculator.classify(new BigDecimal("90.00"), 20, 50, false));
    }
}
