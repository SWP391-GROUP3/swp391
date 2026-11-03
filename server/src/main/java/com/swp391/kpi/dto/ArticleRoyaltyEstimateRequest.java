package com.swp391.kpi.dto;

import com.swp391.kpi.entity.ArticleNature;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * UC-09: uoc tinh nhuan but truoc khi nop. Client gui gia tri dang nhap tren form,
 * dung duoc ca khi chua luu nhap (B16, Quang 01/11).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   20  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class ArticleRoyaltyEstimateRequest {

    @NotNull(message = "Vui lòng chọn thể loại")
    private Integer declaredCategoryId;

    @NotNull(message = "Vui lòng chọn tính chất bài")
    private ArticleNature declaredNature;

    // Khong gui / null = bai khong thuoc su kien nao (ty le bao hoa 100%)
    private Integer eventId;

    // Khong bat buoc (quyet dinh 03/11): bai da tung phan cong BTV (sua va nop lai, UC-10)
    // thi dung ty le bao hoa da khoa lan dau (BR-IN-07), khong dem hang moi
    private Integer articleId;
}
