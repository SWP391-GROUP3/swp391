package com.swp391.kpi.kpi.dto;

import com.swp391.kpi.kpi.entity.KpiTarget;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO trả về cho Frontend hiển thị thông tin chỉ tiêu (UC-59).
 * Map tay bằng static method from() theo quy ước AGENTS.md mục 3.4.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  field                           cac truong tra ve cho giao dien (them ho ten phong vien, ten phong, ten truong ban)
 *   38  from()                          Map thu cong tu Entity sang DTO (khong dung MapStruct/ModelMapper)
 *   50  from(entity, reporterName, sectionName, setByName)   Map co them thong tin ten (de hien thi demo)
 */
@Getter
@Setter
public class KpiTargetResponse {

    private Integer id;
    private Integer reporterId;
    // Ten phong vien (lay tu users.full_name) — de hien thi thay cho ID so
    private String reporterName;
    // Ten phong/ban ma phong vien thuoc (lay tu sections.name) — them cho giang vien xem
    private String sectionName;
    private Short targetYear;
    private Byte targetMonth;
    private Integer minPublished;
    private BigDecimal minAvgScore;
    private Integer setBy;
    // Ten truong ban giao chi tieu (lay tu users.full_name)
    private String setByName;

    /**
     * Map thủ công từ Entity sang DTO (không dùng MapStruct/ModelMapper).
     * Dùng khi không cần thông tin tên (ví dụ: gọi nội bộ).
     */
    public static KpiTargetResponse from(KpiTarget entity) {
        return from(entity, null, null, null);
    }

    /**
     * Map thủ công từ Entity sang DTO, kèm tên phóng viên, tên phòng và tên trưởng ban.
     * Dùng trong getTargetsByPeriod() để frontend hiển thị tên thay cho ID số.
     */
    public static KpiTargetResponse from(KpiTarget entity, String reporterName, String sectionName, String setByName) {
        if (entity == null) {
            return null;
        }
        KpiTargetResponse res = new KpiTargetResponse();
        res.setId(entity.getId());
        res.setReporterId(entity.getReporterId());
        res.setReporterName(reporterName);
        res.setSectionName(sectionName);
        res.setTargetYear(entity.getTargetYear());
        res.setTargetMonth(entity.getTargetMonth());
        res.setMinPublished(entity.getMinPublished());
        res.setMinAvgScore(entity.getMinAvgScore());
        res.setSetBy(entity.getSetBy());
        res.setSetByName(setByName);
        return res;
    }
}
