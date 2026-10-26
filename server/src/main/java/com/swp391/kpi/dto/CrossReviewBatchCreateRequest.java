package com.swp391.kpi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter
public class CrossReviewBatchCreateRequest {
    @NotBlank(message = "Tên đợt không được để trống") @Size(max = 100, message = "Tên đợt tối đa 100 ký tự")
    private String batchName;
    @NotNull(message = "Thiếu ngày bắt đầu") private LocalDate periodStart;
    @NotNull(message = "Thiếu ngày kết thúc") private LocalDate periodEnd;
    @NotNull(message = "Thiếu danh sách bài") @Size(min = 1, max = 1000, message = "Đợt cần từ 1 đến 1000 bài")
    private List<@NotNull @Valid Task> tasks;

    @Getter @Setter
    public static class Task {
        @NotNull(message = "Thiếu phiếu gốc") @Positive(message = "Mã phiếu phải dương") private Integer originalScoringId;
        @NotNull(message = "Thiếu người chấm") @Positive(message = "Mã người chấm phải dương") private Integer reviewerId;
        @Size(max = 1000, message = "Lý do tối đa 1000 ký tự") private String reason;
    }
}
