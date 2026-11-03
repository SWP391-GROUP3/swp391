package com.swp391.kpi.article.dto;

import com.swp391.kpi.article.entity.Article;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Man chi tiet bai (UC-02): noi dung, khai bao, tac gia va lich su trang thai.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  field                           cac truong tra ve cho giao dien
 *   54  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class ArticleDetailResponse {

    private Integer id;
    private String title;
    private String content;
    private Integer authorId;            // tac gia chinh
    private String authorName;           // ho ten tac gia chinh, lay qua UserReadService
    private Integer sectionId;
    private Integer declaredCategoryId;  // the loai PV khai
    private String declaredCategoryName; // ten the loai, lay qua GenreService (null neu the loai da tat)
    private Integer categoryId;          // the loai ap dung
    private String categoryName;
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

    // Cac nut khac cua Iter 2, server tinh san de giao dien khong tu viet lai luat
    private boolean canEdit;       // tac gia chinh, bai DRAFT / REVISION_REQUESTED (BR-IN-01)
    private boolean canSubmit;     // tac gia chinh, bai DRAFT (UC-05)
    private boolean canResubmit;   // tac gia chinh, bai REVISION_REQUESTED (UC-10)
    private boolean canRespond;    // dong tac gia dang PENDING, bai cho xac nhan, con han (UC-12)

    private List<ArticleAuthorResponse> authors;
    private List<ArticleStatusLogResponse> statusLogs;

    public static ArticleDetailResponse from(Article article,
                                             String authorName,
                                             List<ArticleAuthorResponse> authors,
                                             List<ArticleStatusLogResponse> statusLogs,
                                             boolean canWithdraw) {
        ArticleDetailResponse dto = new ArticleDetailResponse();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setContent(article.getContent());
        dto.setAuthorId(article.getAuthorId());
        dto.setAuthorName(authorName);
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
