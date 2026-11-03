package com.swp391.kpi.article.entity;

/**
 * Trang thai xac nhan cua tung tac gia trong bai (cot article_authors.confirm_status).
 * Ten hang trung dung gia tri trong CHECK ck_article_authors_status cua schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   15  PENDING                         chua tra loi
 *   16  ACCEPTED                        dong y ty le (tac gia chinh luon ACCEPTED ngay)
 *   17  REJECTED                        tu choi, bat buoc co reject_reason
 *   18  EXPIRED                         qua han 3 ngay khong tra loi (BR-IN-06)
 */
public enum ArticleAuthorConfirmStatus {
    PENDING,   // chua tra loi
    ACCEPTED,  // dong y ty le (tac gia chinh luon ACCEPTED ngay)
    REJECTED,  // tu choi, bat buoc co reject_reason
    EXPIRED    // qua han 3 ngay khong tra loi (BR-IN-06)
}
