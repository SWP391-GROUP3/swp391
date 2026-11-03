package com.swp391.kpi.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thong tin chi tiet 1 bai viet xuat ban cua phong vien trong ky KPI (UC-62, UC-60).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReporterKpiArticleItemResponse {

    private Integer articleId;
    private String title;
    private Integer categoryId;
    private String nature;
    private LocalDateTime publishedAt;

    // Ty le dong tac gia (%) vi du: 60.00 hoac 40.00
    private BigDecimal sharePercent;

    // So bai quy doi = sharePercent / 100 vi du: 0.60 hoac 0.40
    private BigDecimal convertedArticles;

    // Diem chat luong hieu luc cua bai (thang 0-100)
    private BigDecimal effectiveScore;

    // Luot xem tich luy tu doc gia (reader_interactions)
    private Integer pageViews;
}
