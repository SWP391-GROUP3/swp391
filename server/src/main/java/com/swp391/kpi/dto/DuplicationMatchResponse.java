package com.swp391.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 1 bai bi trung trong ket qua kiem trung noi dung (UC-06, UC-08). B7: chi co tieu de bai bi trung,
 * % va cac doan trung (chu cua bai dang nop), khong tra toan bo bai cua nguoi khac.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  field                           cac truong tra ve cho giao dien
 *   26  of()                            of
 */
@Getter
@Setter
public class DuplicationMatchResponse {

    private Integer matchedArticleId;
    private String matchedTitle;
    private BigDecimal overlapPercent;
    private List<String> passages;

    public static DuplicationMatchResponse of(Integer matchedArticleId, String matchedTitle,
                                              BigDecimal overlapPercent, List<String> passages) {
        DuplicationMatchResponse response = new DuplicationMatchResponse();
        response.setMatchedArticleId(matchedArticleId);
        response.setMatchedTitle(matchedTitle);
        response.setOverlapPercent(overlapPercent);
        response.setPassages(passages);
        return response;
    }
}
