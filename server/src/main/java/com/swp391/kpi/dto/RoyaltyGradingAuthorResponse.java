package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Phan tien bai viet cua 1 tac gia trong phieu cham (dong ARTICLE, GBR-10).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  field                           cac truong tra ve cho giao dien
 *   26  from()                          Map tac gia cua BF-01 + so tien da chia
 */
@Getter
@Setter
public class RoyaltyGradingAuthorResponse {

    private Integer userId;
    private String fullName;
    private Boolean mainAuthor;         // tac gia chinh nhan them phan du lam tron
    private BigDecimal sharePercent;    // 70.00 = 70%
    private BigDecimal amount;          // tien nhan duoc, VND

    // Map tac gia cua BF-01 + so tien da chia
    public static RoyaltyGradingAuthorResponse from(ArticleRoyaltyAuthorResponse author, BigDecimal amount) {
        RoyaltyGradingAuthorResponse response = new RoyaltyGradingAuthorResponse();
        response.setUserId(author.getUserId());
        response.setFullName(author.getFullName());
        response.setMainAuthor(author.isMainAuthor());
        response.setSharePercent(author.getSharePercent());
        response.setAmount(amount);
        return response;
    }
}
