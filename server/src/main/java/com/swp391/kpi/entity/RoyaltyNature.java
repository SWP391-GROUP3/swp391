package com.swp391.kpi.entity;

/**
 * Tinh chat bai khi cham nhuan but (cot royalty_gradings.nature).
 * Ten hang trung dung gia tri trong CHECK ck_royalty_gradings_nature cua schema.sql.
 * Ty le % cua tung loai lay tu system_parameters (NATURE_RATE_...), khong ghi o day.
 *
 * MUC LUC (so dong)
 *   12  gia tri                      - ORIGINAL, TRANSLATION, COMPILATION, REPOST
 */
public enum RoyaltyNature {
    ORIGINAL,     // bai goc
    TRANSLATION,  // bai dich
    COMPILATION,  // bai tong hop
    REPOST        // bai dang lai
}
