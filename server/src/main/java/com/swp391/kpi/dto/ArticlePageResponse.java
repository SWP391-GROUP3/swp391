package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 1 trang ket qua cua danh sach "Bai cua toi" (UC-01).
 * Tra ve DTO rieng thay vi Page cua Spring cho JSON gon va de doc.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class ArticlePageResponse {

    private List<ArticleSummaryResponse> items;
    private int page;            // trang hien tai, bat dau tu 0
    private int size;            // so dong moi trang
    private long totalElements;  // tong so bai
    private int totalPages;
}
