package com.swp391.kpi.dto;

import java.math.BigDecimal;

/**
 * Ty le nhuan but cua 1 tac gia trong 1 bai (article_authors.share_percent). M-D2: cho BF-04 / BF-05 doc
 * qua ArticleService.getAuthors() / getAuthorShares() thay vi goi thang ArticleAuthorRepository.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   12  record                          cac truong khai bao trong ngoac cua record
 */
public record ArticleAuthorShareResponse(Integer articleId, Integer userId, BigDecimal sharePercent) {}
