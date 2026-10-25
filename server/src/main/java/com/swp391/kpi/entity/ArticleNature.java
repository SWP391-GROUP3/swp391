package com.swp391.kpi.entity;

/**
 * Tinh chat bai do tac gia khai bao (cot articles.declared_nature).
 * Ten hang trung dung gia tri trong CHECK ck_articles_nature cua schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   15  ORIGINAL                        sang tac
 *   16  TRANSLATION                     dich
 *   17  COMPILATION                     tong hop
 *   18  REPOST                          dang lai
 */
public enum ArticleNature {
    ORIGINAL,     // sang tac
    TRANSLATION,  // dich
    COMPILATION,  // tong hop
    REPOST        // dang lai
}
