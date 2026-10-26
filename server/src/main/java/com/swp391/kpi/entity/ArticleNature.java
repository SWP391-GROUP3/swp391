package com.swp391.kpi.entity;

/**
 * Tinh chat bai do tac gia khai bao (cot articles.declared_nature).
 * Ten hang trung dung gia tri trong CHECK ck_articles_nature cua schema.sql.
 *
 * MUC LUC (so dong)
 *   11  gia tri                      - ORIGINAL, TRANSLATION, COMPILATION, REPOST
 */
public enum ArticleNature {
    ORIGINAL,     // sang tac
    TRANSLATION,  // dich
    COMPILATION,  // tong hop
    REPOST        // dang lai
}
