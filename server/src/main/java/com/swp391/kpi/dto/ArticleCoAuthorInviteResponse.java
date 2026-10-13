package com.swp391.kpi.dto;

import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleAuthor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Loi moi dong tac gia dang cho nguoi dung tra loi (trang chu Phong vien, UC-12).
 * Gan khop client/src/api/reporterHomeApi.js: { articleId, title, mainAuthorName,
 * sharePercent, confirmDeadline } - tam tra mainAuthorId vi chua co entity User.
 */
@Getter
@Setter
public class ArticleCoAuthorInviteResponse {

    private Integer articleId;
    private String title;
    private Integer mainAuthorId;          // TODO(admin): doi thanh mainAuthorName khi co User
    private BigDecimal sharePercent;       // ty le de nghi cho nguoi dung
    private LocalDateTime confirmDeadline; // phai tra loi truoc han nay (BR-IN-06)

    public static ArticleCoAuthorInviteResponse from(Article article, ArticleAuthor myAuthorRow) {
        ArticleCoAuthorInviteResponse dto = new ArticleCoAuthorInviteResponse();
        dto.setArticleId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setMainAuthorId(article.getAuthorId());
        dto.setSharePercent(myAuthorRow.getSharePercent());
        dto.setConfirmDeadline(myAuthorRow.getConfirmDeadline());
        return dto;
    }
}
