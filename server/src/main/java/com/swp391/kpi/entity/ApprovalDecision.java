package com.swp391.kpi.entity;

/**
 * Quyết định duyệt điểm của Trưởng ban biên tập.
 * Khớp với CHECK (decision IN ('APPROVED','RETURNED')) trong schema.sql.
 */
public enum ApprovalDecision {
    APPROVED,
    RETURNED
}
