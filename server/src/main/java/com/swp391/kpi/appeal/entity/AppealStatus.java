package com.swp391.kpi.appeal.entity;

/**
 * Enum AppealStatus: cac gia tri hop le, trung CHECK trong schema.sql (luu bang
 * @Enumerated(EnumType.STRING)).
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   14  SUBMITTED
 *   15  IN_REVIEW
 *   16  RESOLVED
 */
public enum AppealStatus {
    SUBMITTED,
    IN_REVIEW,
    RESOLVED
}
