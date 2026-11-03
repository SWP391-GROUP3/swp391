package com.swp391.kpi.article.dto;

import com.swp391.kpi.article.entity.Article;
import com.swp391.kpi.article.entity.ArticleAuthor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Loi moi dong tac gia dang cho nguoi dung tra loi (trang chu Phong vien, UC-12).
 * Khop dang du lieu client/src/api/reporterHomeApi.js: { articleId, title, mainAuthorName,
 * sharePercent, confirmDeadline }. Giu them mainAuthorId cho ai can tra theo ma.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  field                           cac truong tra ve cho giao dien
 *   31  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class ArticleCoAuthorInviteResponse {

    private Integer articleId;
    private String title;
    private Integer mainAuthorId;
    private String mainAuthorName;         // ho ten tac gia chinh, lay qua UserReadService
    private BigDecimal sharePercent;       // ty le de nghi cho nguoi dung
    private LocalDateTime confirmDeadline; // phai tra loi truoc han nay (BR-IN-06)

    public static ArticleCoAuthorInviteResponse from(Article article, ArticleAuthor myAuthorRow,
                                                     String mainAuthorName) {
        ArticleCoAuthorInviteResponse dto = new ArticleCoAuthorInviteResponse();
        dto.setArticleId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setMainAuthorId(article.getAuthorId());
        dto.setMainAuthorName(mainAuthorName);
        dto.setSharePercent(myAuthorRow.getSharePercent());
        dto.setConfirmDeadline(myAuthorRow.getConfirmDeadline());
        return dto;
    }
}
