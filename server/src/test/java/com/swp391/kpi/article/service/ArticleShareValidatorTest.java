package com.swp391.kpi.article.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra ArticleShareValidator (BR-IN-02). Class tinh thuan nen test chay
 * khong can Spring, khong can DB. 5 = MAX_AUTHORS_PER_ARTICLE trong database/data.sql.
 */
class ArticleShareValidatorTest {

    private static final Integer MAIN_AUTHOR = 1;

    private final ArticleShareValidator validator = new ArticleShareValidator(5);

    private static List<BigDecimal> shares(String... values) {
        BigDecimal[] list = new BigDecimal[values.length];
        for (int i = 0; i < values.length; i++) {
            list[i] = values[i] == null ? null : new BigDecimal(values[i]);
        }
        return Arrays.asList(list);   // Arrays.asList cho phep phan tu null, List.of thi khong
    }

    @Test
    void onlyMainAuthorWith100PercentIsValid() {
        assertNull(validator.findError(MAIN_AUTHOR, List.of(1), shares("100")));
    }

    @Test
    void sharesWithDecimalsSummingTo100AreValid() {
        assertNull(validator.findError(MAIN_AUTHOR, List.of(1, 2, 7), shares("33.34", "33.33", "33.33")));
    }

    @Test
    void fiveAuthorsIsValidButSixIsRejected() {
        assertNull(validator.findError(MAIN_AUTHOR, List.of(1, 2, 3, 4, 5), shares("20", "20", "20", "20", "20")));

        String error = validator.findError(MAIN_AUTHOR, List.of(1, 2, 3, 4, 5, 6),
                shares("50", "10", "10", "10", "10", "10"));
        assertEquals("Một bài có tối đa 5 tác giả.", error);
    }

    @Test
    void totalNotEqual100IsRejected() {
        String error = validator.findError(MAIN_AUTHOR, List.of(1, 2), shares("60", "30"));
        assertTrue(error.startsWith("Mỗi tác giả phải có tỷ lệ trên 0% và tổng tỷ lệ phải bằng 100%"));
        assertTrue(error.contains("90"));
    }

    @Test
    void zeroOrMissingShareIsRejected() {
        assertTrue(validator.findError(MAIN_AUTHOR, List.of(1, 2), shares("100", "0")).startsWith("Mỗi tác giả"));
        assertTrue(validator.findError(MAIN_AUTHOR, List.of(1, 2), shares("100", null)).startsWith("Mỗi tác giả"));
    }

    @Test
    void duplicateAuthorIsRejected() {
        assertEquals("Mỗi tác giả chỉ được khai một lần.",
                validator.findError(MAIN_AUTHOR, List.of(1, 2, 2), shares("50", "25", "25")));
    }

    @Test
    void listWithoutMainAuthorIsRejected() {
        assertEquals("Danh sách tác giả phải có tác giả chính.",
                validator.findError(MAIN_AUTHOR, List.of(2, 3), shares("50", "50")));
    }
}
