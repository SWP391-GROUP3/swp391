package com.swp391.kpi.royalty.dto;

import com.swp391.kpi.royalty.entity.RoyaltyLine;
import com.swp391.kpi.royalty.entity.RoyaltyLineType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 1 dong tien cua phong vien trong man "Nhuan but cua toi" (UC-57): tien bai (theo ty le dong tac gia),
 * tien anh/video hoac dong dieu chinh.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  field                           cac truong tra ve cho giao dien
 *   28  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class RoyaltyMyLineResponse {
    private Integer lineId;
    private Integer articleId;
    private String articleTitle;
    private RoyaltyLineType lineType;
    private BigDecimal sharePercent;   // chi co voi dong ARTICLE
    private BigDecimal amount;

    public static RoyaltyMyLineResponse from(RoyaltyLine line, String articleTitle) {
        RoyaltyMyLineResponse dto = new RoyaltyMyLineResponse();
        dto.setLineId(line.getId());
        dto.setArticleId(line.getGrading().getArticleId());
        dto.setArticleTitle(articleTitle);
        dto.setLineType(line.getLineType());
        dto.setSharePercent(line.getSharePercent());
        dto.setAmount(line.getAmount());
        return dto;
    }
}
