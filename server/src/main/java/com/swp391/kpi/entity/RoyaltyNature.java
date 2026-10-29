package com.swp391.kpi.entity;

/**
 * Tinh chat bai khi cham nhuan but (cot royalty_gradings.nature).
 * Ten hang trung dung gia tri trong CHECK ck_royalty_gradings_nature cua schema.sql.
 * Ty le % cua tung loai lay tu system_parameters (NATURE_RATE_...), khong ghi o day.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   16  ORIGINAL                        bai goc
 *   17  TRANSLATION                     bai dich
 *   18  COMPILATION                     bai tong hop
 *   19  REPOST                          bai dang lai
 */
public enum RoyaltyNature {
    ORIGINAL,     // bai goc
    TRANSLATION,  // bai dich
    COMPILATION,  // bai tong hop
    REPOST        // bai dang lai
}
