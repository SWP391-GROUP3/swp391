package com.swp391.kpi.royalty.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Phieu chi (bang royalty_payments): moi nguoi 1 phieu moi ky, tao luc Ke toan khoa ky (UC-53).
 * Cong thuc (comment bang trong schema.sql, GBR-12):
 *   gross - debtIn >= 0: paidAmount = gross - debtIn, debtOut = 0
 *   nguoc lai:           paidAmount = 0,              debtOut = debtIn - gross (no chuyen ky sau)
 * CHECK ck_royalty_payments_status: UNPAID thi paidBy/paidAt NULL, PAID thi phai co ca hai.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   39  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "royalty_payments")
@Getter
@Setter
@NoArgsConstructor
public class RoyaltyPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK trong luong BF-04: dung @ManyToOne, LAZY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    private RoyaltyPeriod period;           // ky cua phieu chi

    // FK toi users (luong admin): de Integer, nguoi nhan tien. UNIQUE(period_id, user_id)
    private Integer userId;

    private BigDecimal grossAmount;         // DECIMAL(18,0): tong dong tien cua nguoi do trong ky, co the am
    private BigDecimal debtIn;              // no mang tu phieu ky truoc
    private BigDecimal paidAmount;          // so tien thuc chi, khong am
    private BigDecimal debtOut;             // no chuyen sang ky sau

    @Enumerated(EnumType.STRING)            // luu chu "UNPAID"... xuong DB
    private RoyaltyPaymentStatus status;

    private Integer paidBy;                 // FK toi users: Ke toan ghi nhan chi (UC-54)
    private LocalDateTime paidAt;
}
