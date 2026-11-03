package com.swp391.kpi.appeal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

/**
 * DTO client gui len API. Dung o: AppealReviewController, CrossReviewController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  (khong co ham - chi khai bao du lieu)
 */
@Getter @Setter
public class AppealScorecardRequest {
    @NotNull(message = "Danh sách điểm không được để trống")
    @Size(max = 1000, message = "Có quá nhiều tiêu chí")
    private List<@NotNull @Valid Item> criteria;

    @Getter @Setter
    public static class Item {
        @NotNull(message = "Mã tiêu chí không được để trống")
        @Positive(message = "Mã tiêu chí phải là số dương")
        private Integer criterionId;
        private BigDecimal score;
        @Size(max = 1000, message = "Nhận xét tối đa 1000 ký tự")
        private String comment;
    }
}
