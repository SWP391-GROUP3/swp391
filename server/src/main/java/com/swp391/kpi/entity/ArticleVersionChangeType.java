package com.swp391.kpi.entity;

/**
 * Muc do thay doi cua 1 ban so voi ban truoc (cot article_versions.change_type).
 * Ten hang trung dung gia tri trong CHECK ck_article_versions_change cua schema.sql.
 *
 * MUC LUC: dong 10 INITIAL · dong 11 MINOR · dong 12 MAJOR
 */
public enum ArticleVersionChangeType {
    INITIAL, // ban dau tien
    MINOR,   // sua nho
    MAJOR    // sua lon, phai cham lai diem
}
