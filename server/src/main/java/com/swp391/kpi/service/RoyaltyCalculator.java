package com.swp391.kpi.service;

import com.swp391.kpi.entity.RoyaltyNature;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * May tinh nhuan but (BF-04): he so, tien bai/anh, chia tien cho tac gia.
 * Class tinh toan thuan: khong doc DB, khong co @Service (AGENTS.md muc 3.1, 3.9).
 * Moi nguong va ty le lay tu bang system_parameters: service doc ra roi truyen
 * vao constructor, KHONG viet cung so trong class nay.
 */
public class RoyaltyCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");  // doi phan tram ra ty le
    private static final int FACTOR_SCALE = 2;  // he so luu DECIMAL(5,2): giu 2 chu so thap phan

    private final BigDecimal natureRateOriginal;     // % theo tinh chat bai: bai goc
    private final BigDecimal natureRateTranslation;  // bai dich
    private final BigDecimal natureRateCompilation;  // bai tong hop
    private final BigDecimal natureRateRepost;       // bai dang lai
    private final BigDecimal minFinalFactor;         // he so cuoi toi thieu
    private final BigDecimal roundingUnit;           // don vi lam tron tien (VND)
    private final BigDecimal unitPricePercent;       // don gia 1 he so = bao nhieu % luong co so

    // params: param_key -> param_value, lay tu bang system_parameters.
    // Chuoi trong ngoac kep la DUNG ten param_key trong bang (xem database/data.sql).
    public RoyaltyCalculator(Map<String, String> params) {
        this.natureRateOriginal = readDecimal(params, "NATURE_RATE_ORIGINAL");
        this.natureRateTranslation = readDecimal(params, "NATURE_RATE_TRANSLATION");
        this.natureRateCompilation = readDecimal(params, "NATURE_RATE_COMPILATION");
        this.natureRateRepost = readDecimal(params, "NATURE_RATE_REPOST");
        this.minFinalFactor = readDecimal(params, "MIN_FINAL_FACTOR");
        this.roundingUnit = readDecimal(params, "ROUNDING_UNIT");
        this.unitPricePercent = readDecimal(params, "UNIT_PRICE_PERCENT");
    }

    // Don gia 1 he so = luong co so x UNIT_PRICE_PERCENT / 100, bo phan le duoi 1 dong (UC-55).
    // Vd 2.340.000 x 10 / 100 = 234.000
    public BigDecimal unitPrice(BigDecimal baseSalary) {
        return baseSalary.multiply(unitPricePercent).divide(ONE_HUNDRED, 0, RoundingMode.FLOOR);
    }

    // He so goi y = he so chuan cua the loai x diem / 100 (BR-RY-01, nhom da doi:
    // nhan thang diem thay cho chia bac 90/70). Vd 5.00 x 85 / 100 = 4.25
    public BigDecimal suggestedFactor(BigDecimal baseFactor, BigDecimal score) {
        BigDecimal factor = baseFactor.multiply(score);
        return factor.divide(ONE_HUNDRED, FACTOR_SCALE, RoundingMode.HALF_UP);
    }

    // Ty le bao hoa (%) dung de tinh tien (GBR-09):
    // - bai khong thuoc su kien (articleSaturationRate = null) -> 100
    // - diem >= exempt_score cua su kien -> mien giam, 100
    // - con lai -> ty le da khoa luc nop bai (articles.saturation_rate, BR-IN-03)
    public BigDecimal effectiveSaturationRate(BigDecimal score, BigDecimal articleSaturationRate,
                                              BigDecimal exemptScore) {
        if (articleSaturationRate == null) {
            return ONE_HUNDRED;
        }
        if (score.compareTo(exemptScore) >= 0) {
            return ONE_HUNDRED;
        }
        return articleSaturationRate;
    }

    // Ty le % theo tinh chat bai (BR-RY-02), lay tu system_parameters NATURE_RATE_...
    // Service luu ket qua vao cot royalty_gradings.nature_rate de giai trinh.
    public BigDecimal natureRate(RoyaltyNature nature) {
        return switch (nature) {
            case ORIGINAL -> natureRateOriginal;
            case TRANSLATION -> natureRateTranslation;
            case COMPILATION -> natureRateCompilation;
            case REPOST -> natureRateRepost;
        };
    }

    // He so cuoi = he so x ty le bao hoa / 100 x ty le tinh chat / 100, toi thieu MIN_FINAL_FACTOR (BR-RY-02).
    // Nhan het roi chia 1 lan cho 100 x 100 de chi lam tron 1 lan o cuoi.
    public BigDecimal finalFactor(BigDecimal coefficient, BigDecimal saturationRate, RoyaltyNature nature) {
        BigDecimal product = coefficient.multiply(saturationRate).multiply(natureRate(nature));
        BigDecimal factor = product.divide(ONE_HUNDRED.multiply(ONE_HUNDRED), FACTOR_SCALE, RoundingMode.HALF_UP);
        return factor.max(minFinalFactor);
    }

    // Tien = he so x don gia, lam tron XUONG ve boi so ROUNDING_UNIT (1.000d).
    // Dung cho ca bai viet (he so cuoi) lan anh/video (media_factors.factor).
    // Vd 5.27 x 234.000 = 1.233.180 -> 1.233.000
    public BigDecimal calculateAmount(BigDecimal factor, BigDecimal unitPrice) {
        return roundDown(factor.multiply(unitPrice));
    }

    // Chia tien bai cho cac tac gia (GBR-10). sharePercents: % cua tung tac gia,
    // PHAN TU DAU TIEN la tac gia chinh; ket qua tra ve theo DUNG thu tu do.
    // Dong tac gia lam tron XUONG 1.000d, tac gia chinh nhan phan con lai,
    // nen tong cac phan luon bang dung amount (tong dong ARTICLE = grading.amount).
    public List<BigDecimal> splitAmount(BigDecimal amount, List<BigDecimal> sharePercents) {
        checkSharesAddUpTo100(sharePercents);

        List<BigDecimal> coAuthorParts = new ArrayList<>();
        BigDecimal coAuthorTotal = BigDecimal.ZERO;
        for (int i = 1; i < sharePercents.size(); i++) {
            // chia cho 100 luon ra so huu han nen khong can tham so lam tron o day
            BigDecimal exactPart = amount.multiply(sharePercents.get(i)).divide(ONE_HUNDRED);
            BigDecimal part = roundDown(exactPart);
            coAuthorParts.add(part);
            coAuthorTotal = coAuthorTotal.add(part);
        }

        List<BigDecimal> parts = new ArrayList<>();
        parts.add(amount.subtract(coAuthorTotal));  // tac gia chinh nhan phan con lai
        parts.addAll(coAuthorParts);
        return parts;
    }

    // Lam tron XUONG ve boi so cua ROUNDING_UNIT: chia ra so "nghin dong", bo phan le, nhan lai.
    // Vd 1.233.180 / 1.000 = 1.233,18 -> 1.233 -> 1.233.000
    private BigDecimal roundDown(BigDecimal money) {
        BigDecimal units = money.divide(roundingUnit, 0, RoundingMode.FLOOR);
        return units.multiply(roundingUnit);
    }

    // Tong % cac tac gia phai dung 100 (BF-01 da kiem luc nop bai, day la lop chan cuoi
    // de khong bao gio chia sai tien neu du lieu bi loi).
    private static void checkSharesAddUpTo100(List<BigDecimal> sharePercents) {
        BigDecimal totalShare = BigDecimal.ZERO;
        for (BigDecimal share : sharePercents) {
            totalShare = totalShare.add(share);
        }
        if (totalShare.compareTo(ONE_HUNDRED) != 0) {
            throw new IllegalArgumentException(
                    "Tổng tỷ lệ chia của các tác giả phải bằng 100%, đang là " + totalShare.toPlainString());
        }
    }

    // Lay 1 tham so dang so. Thieu hoac sai dinh dang thi bao loi ngay luc tao,
    // tranh truong hop am tham tinh sai tien.
    private static BigDecimal readDecimal(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null) {
            throw new IllegalStateException("Thiếu tham số hệ thống: " + key);
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Tham số hệ thống " + key + " không phải là số: " + value);
        }
    }
}
