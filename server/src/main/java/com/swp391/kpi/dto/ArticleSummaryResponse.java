package com.swp391.kpi.dto;

import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleAuthor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 1 dong trong danh sach "Bai cua toi" (UC-01).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  hang so                         ten tham so, ten hanh dong, gioi han... (khong viet so ma trong code)
 *   27  field                           cac truong tra ve cho giao dien
 *   41  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class ArticleSummaryResponse {

    // Vai tro cua nguoi dang xem trong bai
    public static final String ROLE_MAIN_AUTHOR = "MAIN_AUTHOR";
    public static final String ROLE_CO_AUTHOR = "CO_AUTHOR";

    private Integer id;
    private String title;
    private Integer categoryId;
    private String categoryName;       // ten the loai, service gan sau khi goi from() (GenreService)
    private String declaredNature;
    private String status;
    private String myRole;             // MAIN_AUTHOR hoac CO_AUTHOR
    private BigDecimal mySharePercent; // ty le cua nguoi dang xem, null neu chua khai bao
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    /**
     * @param myAuthorRow dong cua nguoi dang xem trong article_authors, co the null
     */
    public static ArticleSummaryResponse from(Article article, ArticleAuthor myAuthorRow, Integer currentUserId) {
        ArticleSummaryResponse dto = new ArticleSummaryResponse();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setCategoryId(article.getCategoryId());
        dto.setDeclaredNature(article.getDeclaredNature().name());
        dto.setStatus(article.getStatus().name());
        dto.setSubmittedAt(article.getSubmittedAt());
        dto.setUpdatedAt(article.getUpdatedAt());

        // Tac gia chinh la articles.author_id, con lai la dong tac gia
        if (article.getAuthorId().equals(currentUserId)) {
            dto.setMyRole(ROLE_MAIN_AUTHOR);
        } else {
            dto.setMyRole(ROLE_CO_AUTHOR);
        }
        if (myAuthorRow != null) {
            dto.setMySharePercent(myAuthorRow.getSharePercent());
        }
        return dto;
    }
}
