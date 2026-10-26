package com.swp391.kpi.entity;

/**
 * Ket qua kiem trung anh/video (cot article_attachments.hash_result).
 * Ten hang trung dung gia tri trong CHECK ck_article_attachments_result cua schema.sql.
 * Cot de NULL = chua kiem.
 *
 * MUC LUC: dong 11 NEW · dong 12 DUPLICATE · dong 13 SIMILAR
 */
public enum ArticleHashResult {
    NEW,       // khong trung anh nao
    DUPLICATE, // trung y het file_hash voi 1 anh khac
    SIMILAR    // dhash gan giong (lech <= DHASH_MAX_BITS bit)
}
