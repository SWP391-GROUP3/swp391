package com.swp391.kpi.entity;

/**
 * Quyết định duyệt điểm của Trưởng ban biên tập.
 * Khớp với CHECK (decision IN ('APPROVED','RETURNED')) trong schema.sql.
 *
 * MUC LUC (so dong)
 *   11  gia tri                      - APPROVED, RETURNED
 */
public enum ApprovalDecision {
    APPROVED,
    RETURNED
}
