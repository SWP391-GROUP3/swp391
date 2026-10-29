package com.swp391.kpi.entity;

/**
 * Enum AppealResolutionRule: cac gia tri hop le, trung CHECK trong schema.sql (luu bang
 * @Enumerated(EnumType.STRING)).
 *
 * MUC LUC (so dong - ten - tac dung)
 *  (khong co ham - chi khai bao du lieu)
 */
public enum AppealResolutionRule {
    KEEP_ORIGINAL, USE_RESCORE,
    USE_S1, AVERAGE_S0_S2, AVERAGE_S1_S2, USE_S2_TIE
}
