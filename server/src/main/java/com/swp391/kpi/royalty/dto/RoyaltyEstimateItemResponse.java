package com.swp391.kpi.royalty.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Tien nhuan but uoc tinh o 1 muc diem gia dinh (UC-09). Moi buoc cong thuc
 * deu tra ve de Phong vien thay vi sao ra so tien (BR-RY-01..03).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   20  field                           cac truong tra ve cho giao dien
 *   27  of()                            Gom ket qua tinh o 1 muc diem thanh 1 dong
 */
@Getter
@Setter
public class RoyaltyEstimateItemResponse {

    private BigDecimal score;            // diem gia dinh, thang 0-100
    private BigDecimal suggestedFactor;  // he so chuan the loai x diem / 100
    private BigDecimal saturationRate;   // % bao hoa da xet mien giam theo diem
    private BigDecimal finalFactor;      // he so cuoi, toi thieu 1
    private BigDecimal amount;           // tien phan bai viet, lam tron xuong 1.000d

    // Gom ket qua tinh o 1 muc diem thanh 1 dong
    public static RoyaltyEstimateItemResponse of(BigDecimal score, BigDecimal suggestedFactor,
                                                 BigDecimal saturationRate, BigDecimal finalFactor,
                                                 BigDecimal amount) {
        RoyaltyEstimateItemResponse item = new RoyaltyEstimateItemResponse();
        item.setScore(score);
        item.setSuggestedFactor(suggestedFactor);
        item.setSaturationRate(saturationRate);
        item.setFinalFactor(finalFactor);
        item.setAmount(amount);
        return item;
    }
}
