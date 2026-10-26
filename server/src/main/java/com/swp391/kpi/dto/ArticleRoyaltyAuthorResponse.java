package com.swp391.kpi.dto;

import com.swp391.kpi.entity.ArticleAuthor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 1 tac gia cua bai, dung de BF-04 chia tien nhuan but (GBR-10).
 *
 * MUC LUC (so dong)
 *   20  cac truong: userId, fullName, sharePercent, mainAuthor
 *   28  from(author, fullName, mainAuthorId)
 */
@Getter
@Setter
public class ArticleRoyaltyAuthorResponse {

    private Integer userId;
    private String fullName;
    private BigDecimal sharePercent;    // luu dang phan tram: 70.00 = 70%
    private boolean mainAuthor;         // true = tac gia chinh (articles.author_id)

    /**
     * @param fullName ho ten, lay qua UserReadService
     */
    public static ArticleRoyaltyAuthorResponse from(ArticleAuthor author, String fullName, Integer mainAuthorId) {
        ArticleRoyaltyAuthorResponse dto = new ArticleRoyaltyAuthorResponse();
        dto.setUserId(author.getUserId());
        dto.setFullName(fullName);
        dto.setSharePercent(author.getSharePercent());
        dto.setMainAuthor(author.getUserId().equals(mainAuthorId));
        return dto;
    }
}
