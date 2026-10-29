package com.swp391.kpi.dto;

import com.swp391.kpi.entity.Criteria;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO dữ liệu của 1 tiêu chí thành phần trong bộ tiêu chí.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   28  field                           cac truong tra ve cho giao dien
 *   40  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaItemDTO {

    private Integer id;

    @NotBlank(message = "Tên tiêu chí không được để trống")
    private String name;

    @NotNull(message = "Trọng số tiêu chí không được để trống")
    @DecimalMin(value = "0.01", message = "Trọng số phải lớn hơn 0%")
    @DecimalMax(value = "100.00", message = "Trọng số không được vượt quá 100%")
    private BigDecimal weight;

    private Integer sortOrder = 1;

    public static CriteriaItemDTO from(Criteria entity) {
        if (entity == null) return null;
        return new CriteriaItemDTO(
                entity.getId(),
                entity.getName(),
                entity.getWeight(),
                entity.getSortOrder()
        );
    }
}
