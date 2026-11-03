package com.swp391.kpi.article.entity;

/**
 * Dinh dang file anh/video (cot article_attachments.media_type).
 * Ten hang trung dung gia tri trong CHECK ck_article_attachments_type cua schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   13  IMAGE                           file anh
 *   14  VIDEO                           file video
 */
public enum ArticleMediaType {
    IMAGE,  // file anh
    VIDEO   // file video
}
