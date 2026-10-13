package com.swp391.kpi.dto;

import com.swp391.kpi.entity.Criteria;
import com.swp391.kpi.entity.CriteriaSetStatus;
import com.swp391.kpi.entity.CriteriaSetVersion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Response DTO trả về thông tin chi tiết của 1 phiên bản bộ tiêu chí kèm các tiêu chí thành phần.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CriteriaSetVersionResponse {

    private Integer id;
    private Integer categoryId;
    private Integer versionNo;
    private CriteriaSetStatus status;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private Integer activatedBy;
    private LocalDateTime activatedAt;
    private BigDecimal totalWeight;
    private List<CriteriaItemDTO> criteria;

    public static CriteriaSetVersionResponse from(CriteriaSetVersion version, List<Criteria> criteriaList) {
        if (version == null) return null;

        List<CriteriaItemDTO> itemDTOs = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;

        if (criteriaList != null) {
            for (Criteria c : criteriaList) {
                itemDTOs.add(CriteriaItemDTO.from(c));
                if (c.getWeight() != null) {
                    sum = sum.add(c.getWeight());
                }
            }
        }

        return new CriteriaSetVersionResponse(
                version.getId(),
                version.getCategoryId(),
                version.getVersionNo(),
                version.getStatus(),
                version.getCreatedBy(),
                version.getCreatedAt(),
                version.getActivatedBy(),
                version.getActivatedAt(),
                sum,
                itemDTOs
        );
    }
}
