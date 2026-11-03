package com.swp391.kpi.royalty.dto;

import com.swp391.kpi.royalty.entity.RoyaltyPeriodStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Nhuan but cua phong vien trong 1 ky (UC-57): cac dong tien, tong tien va phieu chi (khi ky da khoa).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   20  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class RoyaltyMyPeriodResponse {
    private Integer periodId;
    private Short periodYear;
    private Byte periodMonth;
    private RoyaltyPeriodStatus periodStatus;
    private BigDecimal totalAmount = BigDecimal.ZERO;          // tong cac dong tien cua nguoi do trong ky
    private RoyaltyPaymentResponse payment;                    // null khi ky chua khoa (chua co phieu chi)
    private List<RoyaltyMyLineResponse> lines = new ArrayList<>();
}
