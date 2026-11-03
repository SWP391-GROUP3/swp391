package com.swp391.kpi.assessment.entity;

/**
 * Quyết định đánh giá của Biên tập viên sau khi chấm bài.
 * Khớp với CHECK (decision IN ('ACCEPT','REVISION','REJECT')) trong schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   14  ACCEPT
 *   15  REVISION
 *   16  REJECT
 */
public enum ScoringDecision {
    ACCEPT,
    REVISION,
    REJECT
}
