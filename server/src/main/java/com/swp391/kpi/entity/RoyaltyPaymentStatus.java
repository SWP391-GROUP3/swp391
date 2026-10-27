package com.swp391.kpi.entity;

/**
 * Trang thai phieu chi (cot royalty_payments.status).
 * Ten hang trung dung gia tri trong CHECK ck_royalty_payments_status cua schema.sql.
 *
 * MUC LUC (so dong)
 *   11  gia tri                      - UNPAID, PAID
 */
public enum RoyaltyPaymentStatus {
    UNPAID,  // tao luc Ke toan khoa ky (UC-53), chua chi
    PAID     // Ke toan da ghi nhan chi (UC-54)
}
