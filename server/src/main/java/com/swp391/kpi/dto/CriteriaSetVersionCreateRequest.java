package com.swp391.kpi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Request DTO tạo phiên bản bộ tiêu chí mới (UC-15, UC-16).
 *
 * MUC LUC (so dong)
 *   24  field                        - cac truong tra ve cho giao dien
 */
@Getter
@Setter
@NoArgsConstructor
public class CriteriaSetVersionCreateRequest {

    @NotNull(message = "Mã thể loại bài viết không được để trống")
    private Integer categoryId;

    private Integer createdBy;

    // Tùy chọn kích hoạt ngay sau khi tạo
    private Boolean autoActivate = false;

    @NotEmpty(message = "Danh sách tiêu chí không được để trống")
    @Valid
    private List<CriteriaItemDTO> criteria;
}
