package com.swp391.kpi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AppealCreateRequest {

    @NotNull(message = "Bài viết không được để trống")
    @jakarta.validation.constraints.Positive(message = "Mã bài viết phải lớn hơn 0")
    private Integer articleId;

    @Valid
    @NotEmpty(message = "Phải chọn ít nhất một tiêu chí và nhập lý do cho từng tiêu chí")
    private List<@NotNull(message = "Tiêu chí khiếu nại không được để trống") AppealCriterionRequest> criteria;
}
