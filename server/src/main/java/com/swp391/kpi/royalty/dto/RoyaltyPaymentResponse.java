package com.swp391.kpi.royalty.dto;

import com.swp391.kpi.royalty.entity.RoyaltyPayment;
import com.swp391.kpi.royalty.entity.RoyaltyPaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Phieu chi cua 1 nguoi trong 1 ky (UC-53 tao, UC-54 Ke toan ghi nhan chi, UC-57 PV xem).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   23  field                           cac truong tra ve cho giao dien
 *   37  from()                          doi entity sang DTO, kem ho ten nguoi nhan
 */
@Getter
@Setter
public class RoyaltyPaymentResponse {
    private Integer id;
    private Integer periodId;
    private Integer userId;
    private String fullName;
    private BigDecimal grossAmount;   // tong dong tien cua nguoi do trong ky
    private BigDecimal debtIn;        // no mang tu ky truoc
    private BigDecimal paidAmount;    // so tien thuc chi
    private BigDecimal debtOut;       // no chuyen sang ky sau
    private RoyaltyPaymentStatus status;
    private Integer paidBy;
    private LocalDateTime paidAt;

    // Lay periodId qua getPeriod().getId(): Hibernate da biet san id, khong can query them
    public static RoyaltyPaymentResponse from(RoyaltyPayment payment, String fullName) {
        RoyaltyPaymentResponse dto = new RoyaltyPaymentResponse();
        dto.setId(payment.getId());
        dto.setPeriodId(payment.getPeriod().getId());
        dto.setUserId(payment.getUserId());
        dto.setFullName(fullName);
        dto.setGrossAmount(payment.getGrossAmount());
        dto.setDebtIn(payment.getDebtIn());
        dto.setPaidAmount(payment.getPaidAmount());
        dto.setDebtOut(payment.getDebtOut());
        dto.setStatus(payment.getStatus());
        dto.setPaidBy(payment.getPaidBy());
        dto.setPaidAt(payment.getPaidAt());
        return dto;
    }
}
