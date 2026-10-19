package com.swp391.kpi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Request DTO cho BTV doi the loai bai viet truoc khi cham (UC-23, BR-SC-05)
@Getter
@Setter
@NoArgsConstructor
public class CategoryChangeRequest {

    @NotNull(message = "Mã thể loại mới không được để trống")
    private Integer newCategoryId;

    public CategoryChangeRequest(Integer newCategoryId) {
        this.newCategoryId = newCategoryId;
    }
}
