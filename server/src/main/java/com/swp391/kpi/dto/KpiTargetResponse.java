package com.swp391.kpi.dto;

import com.swp391.kpi.entity.KpiTarget;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO trả về cho Frontend hiển thị thông tin chỉ tiêu (UC-59).
 * Map tay bằng static method from() theo quy ước AGENTS.md mục 3.4.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  field                           cac truong tra ve cho giao dien
 *   32  from()                          Map thu cong tu Entity sang DTO (khong dung MapStruct/ModelMapper)
 */
@Getter
@Setter
public class KpiTargetResponse {

    private Integer id;
    private Integer reporterId;
    private Short targetYear;
    private Byte targetMonth;
    private Integer minPublished;
    private BigDecimal minAvgScore;
    private Integer setBy;

    /**
     * Map thủ công từ Entity sang DTO (không dùng MapStruct/ModelMapper).
     */
    public static KpiTargetResponse from(KpiTarget entity) {
        if (entity == null) {
            return null;
        }
        KpiTargetResponse res = new KpiTargetResponse();
        res.setId(entity.getId());
        res.setReporterId(entity.getReporterId());
        res.setTargetYear(entity.getTargetYear());
        res.setTargetMonth(entity.getTargetMonth());
        res.setMinPublished(entity.getMinPublished());
        res.setMinAvgScore(entity.getMinAvgScore());
        res.setSetBy(entity.getSetBy());
        return res;
    }
}
