package com.swp391.kpi.article.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 1 tac gia + ty le khi khai bao (UC-04). Danh sach gom CA tac gia chinh.
 * Luat > 0%, tong 100%, toi da 5 nguoi kiem o service (BR-IN-02).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class ArticleAuthorShareRequest {

    @NotNull(message = "Chưa chọn tác giả")
    private Integer userId;

    @NotNull(message = "Chưa nhập tỷ lệ của tác giả")
    private BigDecimal sharePercent;   // dang phan tram, vd 60 = 60%
}
