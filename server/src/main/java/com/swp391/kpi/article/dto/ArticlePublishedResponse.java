package com.swp391.kpi.article.dto;

import com.swp391.kpi.article.repository.ArticlePublishedRow;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 1 bai da dang, cho BF-04 lam danh sach bai cho cham nhuan but (UC-46).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   20  field                           cac truong tra ve cho giao dien
 *   34  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class ArticlePublishedResponse {

    private Integer id;
    private String title;
    private Integer authorId;           // tac gia chinh (articles.author_id)
    private String authorName;          // users.full_name cua tac gia chinh
    private Integer sectionId;          // ban cua tac gia chinh luc tao bai
    private String sectionName;
    private Integer categoryId;         // the loai AP DUNG (articles.category_id), khong phai the loai khai
    private String categoryName;
    private String declaredNature;      // ORIGINAL / TRANSLATION / COMPILATION / REPOST
    private LocalDateTime publishedAt;

    /**
     * @param authorName ho ten tac gia chinh, lay qua UserReadService (khong query bang users o day)
     */
    public static ArticlePublishedResponse from(ArticlePublishedRow row, String authorName) {
        ArticlePublishedResponse dto = new ArticlePublishedResponse();
        dto.setId(row.getId());
        dto.setTitle(row.getTitle());
        dto.setAuthorId(row.getAuthorId());
        dto.setAuthorName(authorName);
        dto.setSectionId(row.getSectionId());
        dto.setSectionName(row.getSectionName());
        dto.setCategoryId(row.getCategoryId());
        dto.setCategoryName(row.getCategoryName());
        dto.setDeclaredNature(row.getDeclaredNature());
        dto.setPublishedAt(row.getPublishedAt());
        return dto;
    }
}
