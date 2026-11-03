package com.swp391.kpi.royalty.dto;

import com.swp391.kpi.royalty.entity.RoyaltyNature;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ket qua uoc tinh nhuan but truoc khi nop bai (UC-09, BF-01 hien cho Phong vien).
 * Bai chua co diem nen tra ve nhieu muc diem gia dinh (items); chi tinh phan bai viet,
 * chua tinh anh/video.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  field                          cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class RoyaltyEstimateResponse {

    private Integer categoryId;
    private BigDecimal baseFactor;     // he so chuan cua the loai
    private RoyaltyNature nature;      // tinh chat bai PV khai
    private BigDecimal natureRate;     // % theo tinh chat (BR-RY-02)
    private BigDecimal unitPrice;      // don gia 1 he so hieu luc hom nay
    private List<RoyaltyEstimateItemResponse> items;  // moi muc diem gia dinh 1 dong, diem cao len dau
}
