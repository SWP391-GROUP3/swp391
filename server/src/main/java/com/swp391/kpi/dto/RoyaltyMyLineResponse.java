package com.swp391.kpi.dto;

import com.swp391.kpi.entity.RoyaltyLine;
import com.swp391.kpi.entity.RoyaltyLineType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 1 dong tien cua phong vien trong man "Nhuan but cua toi" (UC-57): tien bai (theo ty le dong tac gia),
 * tien anh/video hoac dong dieu chinh.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  field                           cac truong tra ve cho giao dien
 *   30  from()                          doi entity sang DTO, kem tieu de bai
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
