package com.swp391.kpi.entity;

/**
 * Trang thai xac nhan cua tung tac gia trong bai (cot article_authors.confirm_status).
 * Ten hang trung dung gia tri trong CHECK ck_article_authors_status cua schema.sql.
 */
public enum ArticleAuthorConfirmStatus {
    PENDING,   // chua tra loi
    ACCEPTED,  // dong y ty le (tac gia chinh luon ACCEPTED ngay)
    REJECTED,  // tu choi, bat buoc co reject_reason
    EXPIRED    // qua han 3 ngay khong tra loi (BR-IN-06)
}
