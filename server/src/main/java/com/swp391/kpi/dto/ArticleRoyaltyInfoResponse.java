package com.swp391.kpi.dto;

import com.swp391.kpi.entity.Article;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Thong tin 1 bai da dang de BF-04 cham nhuan but (UC-47, UC-48).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  field                           cac truong tra ve cho giao dien
 *   33  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class ArticleRoyaltyInfoResponse {

    private Integer id;
    private String title;
    private Integer categoryId;         // the loai ap dung -> BF-04 lay base_factor (BR-RY-01)
    private String declaredNature;      // tinh chat PV khai, dung thang de tinh nhuan but (BR-IN-03)
    private Integer eventId;            // null = khong thuoc su kien
    private BigDecimal saturationRate;  // ty le bao hoa da khoa luc nop (BR-IN-03), null = khong thuoc su kien
    private LocalDateTime publishedAt;  // chon don gia hieu luc tai ngay dang (GBR-08)

    private List<ArticleRoyaltyAuthorResponse> authors;
    private List<ArticleRoyaltyAttachmentResponse> attachments;

    public static ArticleRoyaltyInfoResponse from(Article article,
                                                  List<ArticleRoyaltyAuthorResponse> authors,
                                                  List<ArticleRoyaltyAttachmentResponse> attachments) {
        ArticleRoyaltyInfoResponse dto = new ArticleRoyaltyInfoResponse();
        dto.setId(article.getId());
        dto.setTitle(article.getTitle());
        dto.setCategoryId(article.getCategoryId());
        dto.setDeclaredNature(article.getDeclaredNature().name());
        dto.setEventId(article.getEventId());
        dto.setSaturationRate(article.getSaturationRate());
        dto.setPublishedAt(article.getPublishedAt());
        dto.setAuthors(authors);
        dto.setAttachments(attachments);
        return dto;
    }
}
