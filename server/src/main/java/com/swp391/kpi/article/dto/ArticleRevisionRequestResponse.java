package com.swp391.kpi.article.dto;

import com.swp391.kpi.article.entity.Article;
import com.swp391.kpi.article.entity.ArticleStatusLog;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Bai bi tra ve can sua (REVISION_REQUESTED) kem ghi chu moi nhat (trang chu Phong vien).
 * Dang du lieu khop client/src/api/reporterHomeApi.js: { articleId, title, note, returnedAt }.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  field                           cac truong tra ve cho giao dien
 *   30  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class ArticleRevisionRequestResponse {

    private Integer articleId;
    private String title;
    private String note;              // ghi chu cua BTV luc tra bai (article_status_logs.note)
    private LocalDateTime returnedAt; // luc bi tra ve

    /**
     * @param returnLog dong lich su luc bai chuyen sang REVISION_REQUESTED, co the null
     */
    public static ArticleRevisionRequestResponse from(Article article, ArticleStatusLog returnLog) {
        ArticleRevisionRequestResponse dto = new ArticleRevisionRequestResponse();
        dto.setArticleId(article.getId());
        dto.setTitle(article.getTitle());
        if (returnLog != null) {
            dto.setNote(returnLog.getNote());
            dto.setReturnedAt(returnLog.getCreatedAt());
        }
        return dto;
    }
}
