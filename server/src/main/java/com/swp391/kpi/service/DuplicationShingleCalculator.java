package com.swp391.kpi.service;

import com.swp391.kpi.entity.DuplicationResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Tinh trung noi dung theo cum am tiet (UC-06, BR-IN-04, cach hieu B1-B4 nhom truong OK 01/11).
 * Logic tinh thuan, khong dung Spring/DB (AGENTS.md muc 3.9) -> co unit test rieng.
 *  - B1: chu thuong, bo dau cau, giu dau tieng Viet, tach theo khoang trang; 1 cum = SHINGLE_SIZE am tiet lien nhau.
 *  - B2: % = so cum cua bai dang nop co trong bai kia / tong so cum cua bai dang nop x 100.
 *  - B4: doan trung = gop cac cum trung lien nhau thanh 1 doan.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   31  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   36  splitWords()                    Tach noi dung thanh cac am tiet, giu nguyen chu goc (de hien doan trung)
 *   50  normalize()                     B1: 1 am tiet -> chu thuong, bo dau cau, giu dau tieng Viet (\p{M} la dau thanh neu go kieu to hop)
 *   56  shingles()                      Cac cum size am tiet lien nhau (cum thu i bat dau o am tiet thu i)
 *   72  overlapPercent()                B2: % cum cua bai dang nop co trong bai kia, lam tron 2 chu so
 *   87  passages()                      B4: cac doan trung (chu goc cua bai dang nop), gop cac cum trung lien nhau / chong nhau thanh 1 doan
 *  119  classify()                      BR-IN-04 (comment bang duplication_checks): duoi warn% NORMAL; tren block% va bai Sang tac BLOCKED; con lai (…
 */
public class DuplicationShingleCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int PERCENT_SCALE = 2;   // duplication_checks.max_overlap_percent DECIMAL(5,2)

    // Tach noi dung thanh cac am tiet, giu nguyen chu goc (de hien doan trung).
    // Tu chi gom dau cau (vd "-", "...") thi bo qua.
    public static List<String> splitWords(String text) {
        List<String> words = new ArrayList<>();
        if (text == null) {
            return words;
        }
        for (String word : text.trim().split("\\s+")) {
            if (!normalize(word).isEmpty()) {
                words.add(word);
            }
        }
        return words;
    }

    // B1: 1 am tiet -> chu thuong, bo dau cau, giu dau tieng Viet (\p{M} la dau thanh neu go kieu to hop)
    public static String normalize(String word) {
        String text = Normalizer.normalize(word, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        return text.replaceAll("[^\\p{L}\\p{M}\\p{N}]", "");
    }

    // Cac cum size am tiet lien nhau (cum thu i bat dau o am tiet thu i). It hon size am tiet -> rong (B3: 0%)
    public static List<String> shingles(List<String> words, int size) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i + size <= words.size(); i++) {
            StringBuilder shingle = new StringBuilder();
            for (int j = i; j < i + size; j++) {
                if (j > i) {
                    shingle.append(' ');
                }
                shingle.append(normalize(words.get(j)));
            }
            result.add(shingle.toString());
        }
        return result;
    }

    // B2: % cum cua bai dang nop co trong bai kia, lam tron 2 chu so
    public static BigDecimal overlapPercent(List<String> myShingles, Set<String> otherShingles) {
        if (myShingles.isEmpty()) {
            return BigDecimal.ZERO.setScale(PERCENT_SCALE);
        }
        int matched = 0;
        for (String shingle : myShingles) {
            if (otherShingles.contains(shingle)) {
                matched++;
            }
        }
        return BigDecimal.valueOf(matched).multiply(HUNDRED)
                .divide(BigDecimal.valueOf(myShingles.size()), PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    // B4: cac doan trung (chu goc cua bai dang nop), gop cac cum trung lien nhau / chong nhau thanh 1 doan
    public static List<String> passages(List<String> words, List<String> myShingles, Set<String> otherShingles, int size) {
        // 1. Danh dau am tiet nam trong it nhat 1 cum trung
        boolean[] covered = new boolean[words.size()];
        for (int i = 0; i < myShingles.size(); i++) {
            if (otherShingles.contains(myShingles.get(i))) {
                for (int j = i; j < i + size; j++) {
                    covered[j] = true;
                }
            }
        }
        // 2. Cac am tiet duoc danh dau lien nhau -> 1 doan
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            if (covered[i]) {
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(words.get(i));
            } else if (current.length() > 0) {
                result.add(current.toString());
                current.setLength(0);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    // BR-IN-04 (comment bang duplication_checks): duoi warn% NORMAL; tren block% va bai Sang tac BLOCKED;
    // con lai (tu warn% den block%, hoac tren block% nhung khong phai Sang tac) NEED_EXPLAIN
    public static DuplicationResult classify(BigDecimal maxPercent, int warnPercent, int blockPercent, boolean original) {
        if (maxPercent.compareTo(BigDecimal.valueOf(warnPercent)) < 0) {
            return DuplicationResult.NORMAL;
        }
        if (maxPercent.compareTo(BigDecimal.valueOf(blockPercent)) > 0 && original) {
            return DuplicationResult.BLOCKED;
        }
        return DuplicationResult.NEED_EXPLAIN;
    }
}
