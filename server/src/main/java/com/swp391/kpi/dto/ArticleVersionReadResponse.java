package com.swp391.kpi.dto;

/**
 * DTO tra ve cho giao dien. Dung o: ArticleReadService.
 *
 * MUC LUC (so dong - ten - tac dung)
 *    9  record                          cac truong khai bao trong ngoac cua record
 */
public record ArticleVersionReadResponse(Integer id, Integer articleId, String title, String content) {}
