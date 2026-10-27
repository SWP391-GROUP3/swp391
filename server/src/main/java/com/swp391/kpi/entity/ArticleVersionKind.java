package com.swp391.kpi.entity;

/**
 * Loai ban chup noi dung (cot article_versions.kind).
 * Ten hang trung dung gia tri trong CHECK ck_article_versions_kind cua schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   13  SUBMISSION                      ban Phong vien nop (moi lan nop / nop lai)
 *   14  EDITED                          ban Bien tap vien bien tap (BF-02)
 */
public enum ArticleVersionKind {
    SUBMISSION, // ban Phong vien nop (moi lan nop / nop lai)
    EDITED      // ban Bien tap vien bien tap (BF-02)
}
