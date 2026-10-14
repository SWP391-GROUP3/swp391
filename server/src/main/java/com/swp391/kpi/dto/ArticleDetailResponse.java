package com.swp391.kpi.dto;

import com.swp391.kpi.entity.Article;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Man chi tiet bai (UC-02): noi dung, khai bao, tac gia va lich su trang thai.
 */
@Getter
@Setter
public class ArticleDetailResponse {

    private Integer id;
    private String title;
    private String content;
    private Integer authorId;            // tac gia chinh. TODO(admin): tra them ho ten
    private Integer sectionId;
    private Integer declaredCategoryId;  // the loai PV khai
    private Integer categoryId;          // the loai ap dung
    private Integer eventId;
    private String declaredNature;
    private String status;
    private BigDecimal saturationRate;
    private LocalDateTime submittedAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Nguoi dang xem co duoc bam "Rut bai" khong (BR-IN-08), de giao dien an/hien nut
    private boolean canWithdraw;

    private List<ArticleAuthorResponse> authors;
    private List<ArticleStatusLogResponse> statusLogs;

    public static ArticleDetailResponse from(Article article,
                                             List<ArticleAuthorResponse> authors,
                                             List<ArticleStatusLogResponse> statusLogs,
                                             boolean canWithdraw) {
        ArticleDetailResponse dto = new ArticleDetailResponse();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setContent(article.getContent());
        dto.setAuthorId(article.getAuthorId());
        dto.setSectionId(article.getSectionId());
        dto.setDeclaredCategoryId(article.getDeclaredCategoryId());
        dto.setCategoryId(article.getCategoryId());
        dto.setEventId(article.getEventId());
        dto.setDeclaredNature(article.getDeclaredNature().name());
        dto.setStatus(article.getStatus().name());
        dto.setSaturationRate(article.getSaturationRate());
        dto.setSubmittedAt(article.getSubmittedAt());
        dto.setWithdrawnAt(article.getWithdrawnAt());
        dto.setPublishedAt(article.getPublishedAt());
        dto.setCreatedAt(article.getCreatedAt());
        dto.setUpdatedAt(article.getUpdatedAt());
        dto.setCanWithdraw(canWithdraw);
        dto.setAuthors(authors);
        dto.setStatusLogs(statusLogs);
        return dto;
    }
}
