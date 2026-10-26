package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 1 bai da dang CHUA cham nhuan but, cho man Pending Grading List (UC-46).
 *
 * MUC LUC (so dong)
 *   20  field                        - cac truong tra ve cho giao dien
 *   34  from()                       - Map tay DTO cua BF-01 -> DTO cua BF-04 (AGENTS.md muc 3.4), them 3 so l…
 */
@Getter
@Setter
public class RoyaltyPendingArticleResponse {

    private Integer articleId;
    private String title;
    private String authorName;        // tac gia chinh
    private Integer sectionId;        // de frontend loc theo ban
    private String sectionName;
    private Integer categoryId;       // de frontend loc theo the loai
    private String categoryName;
    private String declaredNature;    // tinh chat PV khai, dung luon de tinh nhuan but (BR-IN-03)
    private LocalDateTime publishedAt;
    private LocalDate paymentDeadline;  // han chi = ngay dang + PAYMENT_DEADLINE_DAYS
    private Long daysLeft;              // so ngay con lai toi han chi, am = da qua han
    private Boolean nearDeadline;       // true khi daysLeft <= PAYMENT_WARN_DAYS (gom ca qua han)

    // Map tay DTO cua BF-01 -> DTO cua BF-04 (AGENTS.md muc 3.4), them 3 so lieu han chi
    public static RoyaltyPendingArticleResponse from(ArticlePublishedResponse article, LocalDate paymentDeadline,
                                                     long daysLeft, boolean nearDeadline) {
        RoyaltyPendingArticleResponse response = new RoyaltyPendingArticleResponse();
        response.setArticleId(article.getId());
        response.setTitle(article.getTitle());
        response.setAuthorName(article.getAuthorName());
        response.setSectionId(article.getSectionId());
        response.setSectionName(article.getSectionName());
        response.setCategoryId(article.getCategoryId());
        response.setCategoryName(article.getCategoryName());
        response.setDeclaredNature(article.getDeclaredNature());
        response.setPublishedAt(article.getPublishedAt());
        response.setPaymentDeadline(paymentDeadline);
        response.setDaysLeft(daysLeft);
        response.setNearDeadline(nearDeadline);
        return response;
    }
}
