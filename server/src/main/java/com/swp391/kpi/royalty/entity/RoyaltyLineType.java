package com.swp391.kpi.royalty.entity;

/**
 * Loai dong tien nhuan but (cot royalty_lines.line_type).
 * Ten hang trung dung gia tri trong CHECK ck_royalty_lines_type cua schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   14  ARTICLE                         tien phan bai viet, chia theo ty le tac gia (BR-RY-03)
 *   15  MEDIA                           tien 1 anh/video, tra cho tac gia anh (BR-RY-04)
 *   16  ADJUSTMENT                      chenh lech khi tinh lai bai cua ky da LOCKED, ghi vao ky sau, co the am (BR-RY-06)
 */
public enum RoyaltyLineType {
    ARTICLE,     // tien phan bai viet, chia theo ty le tac gia (BR-RY-03)
    MEDIA,       // tien 1 anh/video, tra cho tac gia anh (BR-RY-04)
    ADJUSTMENT   // chenh lech khi tinh lai bai cua ky da LOCKED, ghi vao ky sau, co the am (BR-RY-06)
}
