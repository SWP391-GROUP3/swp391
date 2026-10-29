package com.swp391.kpi.entity;

/**
 * Loại lần chấm bài viết.
 * Khớp với CHECK (attempt_type IN ('INITIAL','RESCORE','APPEAL')) trong schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   14  INITIAL
 *   15  RESCORE
 *   16  APPEAL
 */
public enum AttemptType {
    INITIAL,
    RESCORE,
    APPEAL
}
