package com.swp391.kpi.royalty.entity;

/**
 * Trang thai phieu chi (cot royalty_payments.status).
 * Ten hang trung dung gia tri trong CHECK ck_royalty_payments_status cua schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   13  UNPAID                          tao luc Ke toan khoa ky (UC-53), chua chi
 *   14  PAID                            Ke toan da ghi nhan chi (UC-54)
 */
public enum RoyaltyPaymentStatus {
    UNPAID,  // tao luc Ke toan khoa ky (UC-53), chua chi
    PAID     // Ke toan da ghi nhan chi (UC-54)
}
