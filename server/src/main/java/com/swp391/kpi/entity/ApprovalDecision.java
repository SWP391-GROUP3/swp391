package com.swp391.kpi.entity;

/**
 * Quyết định duyệt điểm của Trưởng ban biên tập.
 * Khớp với CHECK (decision IN ('APPROVED','RETURNED')) trong schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   13  APPROVED
 *   14  RETURNED
 */
public enum ApprovalDecision {
    APPROVED,
    RETURNED
}
