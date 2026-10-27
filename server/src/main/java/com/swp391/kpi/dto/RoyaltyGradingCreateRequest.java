package com.swp391.kpi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Thu ky bam Hoan tat cham nhuan but 1 bai (UC-47 buoc 4).
 * Chi gui bai nao; moi con so khac he thong tu tinh (BR-RY-01), tinh chat bai lay theo PV khai (BR-IN-03).
 * Cham lai bai da cham (phien ban 2 tro di) thi bat buoc them ly do (CHECK ck_royalty_gradings_recalc).
 *
 * MUC LUC (so dong)
 *   20  field                        - cac truong client gui len
 */
@Getter
@Setter
public class RoyaltyGradingCreateRequest {

    @NotNull(message = "Vui lòng chọn bài cần chấm nhuận bút")
    private Integer articleId;

    // Chi can khi cham lai; lan dau de trong. Bat buoc hay khong do service kiem.
    @Size(max = 500, message = "Lý do chấm lại tối đa {max} ký tự")
    private String recalcReason;
}
