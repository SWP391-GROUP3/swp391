package com.swp391.kpi.dto;

import com.swp391.kpi.entity.RoyaltyNature;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Thu ky bam Hoan tat cham nhuan but 1 bai (UC-47 buoc 5).
 * Chi gui bai nao + tinh chat Thu ky xac nhan; moi con so khac he thong tu tinh (BR-RY-01).
 */
@Getter
@Setter
public class RoyaltyGradingCreateRequest {

    @NotNull(message = "Vui lòng chọn bài cần chấm nhuận bút")
    private Integer articleId;

    @NotNull(message = "Vui lòng xác nhận tính chất bài")
    private RoyaltyNature nature;   // ORIGINAL / TRANSLATION / COMPILATION / REPOST
}
