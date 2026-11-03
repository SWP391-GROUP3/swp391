package com.swp391.kpi.article.entity;

/**
 * Muc do thay doi cua 1 ban so voi ban truoc (cot article_versions.change_type).
 * Ten hang trung dung gia tri trong CHECK ck_article_versions_change cua schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   14  INITIAL                         ban dau tien
 *   15  MINOR                           sua nho
 *   16  MAJOR                           sua lon, phai cham lai diem
 */
public enum ArticleVersionChangeType {
    INITIAL, // ban dau tien
    MINOR,   // sua nho
    MAJOR    // sua lon, phai cham lai diem
}
