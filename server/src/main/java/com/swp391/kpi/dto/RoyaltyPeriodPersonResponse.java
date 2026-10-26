package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Tong tien cua 1 nguoi trong ky (UC-50 "total per person"), cong tu cac dong royalty_lines.
 *
 * MUC LUC (so dong)
 *   18  field                        - cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class RoyaltyPeriodPersonResponse {

    private Integer userId;
    private String fullName;
    private BigDecimal articleAmount = BigDecimal.ZERO;     // cong dong ARTICLE
    private BigDecimal mediaAmount = BigDecimal.ZERO;       // cong dong MEDIA
    private BigDecimal adjustmentAmount = BigDecimal.ZERO;  // cong dong ADJUSTMENT (Iter 3, co the am)
    private BigDecimal totalAmount = BigDecimal.ZERO;       // tong 3 loai
}
