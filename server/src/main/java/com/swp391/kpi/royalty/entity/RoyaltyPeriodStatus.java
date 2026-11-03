package com.swp391.kpi.royalty.entity;

/**
 * Trang thai ky nhuan but (cot royalty_periods.status, BR-RY-07).
 * Ten hang trung dung gia tri trong CHECK ck_royalty_periods_status cua schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   15  OPEN                            dang nhan bai cham xong
 *   16  CLOSED                          Thu ky da chot, cho Ke toan kiem
 *   17  LOCKED                          Ke toan da khoa: so lieu bat bien (BR-RY-06)
 *   18  PAID                            da chi het cho moi nguoi
 */
public enum RoyaltyPeriodStatus {
    OPEN,    // dang nhan bai cham xong
    CLOSED,  // Thu ky da chot, cho Ke toan kiem
    LOCKED,  // Ke toan da khoa: so lieu bat bien (BR-RY-06)
    PAID     // da chi het cho moi nguoi
}
