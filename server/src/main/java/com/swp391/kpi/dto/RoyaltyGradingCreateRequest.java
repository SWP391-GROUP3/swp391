package com.swp391.kpi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Thu ky bam Hoan tat cham nhuan but 1 bai (UC-47 buoc 4).
 * Chi gui bai nao; moi con so khac he thong tu tinh (BR-RY-01), tinh chat bai lay theo PV khai (BR-IN-03).
 *
 * MUC LUC (so dong)
 *   18  field                        - cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class RoyaltyGradingCreateRequest {

    @NotNull(message = "Vui lòng chọn bài cần chấm nhuận bút")
    private Integer articleId;
}
