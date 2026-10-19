package com.swp391.kpi.entity;

/**
 * Quyết định đánh giá của Biên tập viên sau khi chấm bài.
 * Khớp với CHECK (decision IN ('ACCEPT','REVISION','REJECT')) trong schema.sql.
 */
public enum ScoringDecision {
    ACCEPT,
    REVISION,
    REJECT
}
