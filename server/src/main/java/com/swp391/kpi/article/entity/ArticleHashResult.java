package com.swp391.kpi.article.entity;

/**
 * Ket qua kiem trung anh/video (cot article_attachments.hash_result).
 * Ten hang trung dung gia tri trong CHECK ck_article_attachments_result cua schema.sql.
 * Cot de NULL = chua kiem.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   15  NEW                             khong trung anh nao
 *   16  DUPLICATE                       trung y het file_hash voi 1 anh khac
 *   17  SIMILAR                         dhash gan giong (lech <= DHASH_MAX_BITS bit)
 */
public enum ArticleHashResult {
    NEW,       // khong trung anh nao
    DUPLICATE, // trung y het file_hash voi 1 anh khac
    SIMILAR    // dhash gan giong (lech <= DHASH_MAX_BITS bit)
}
