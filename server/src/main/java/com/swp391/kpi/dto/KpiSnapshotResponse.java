package com.swp391.kpi.dto;

import com.swp391.kpi.entity.KpiSnapshot;
import com.swp391.kpi.entity.KpiSnapshotStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phan hoi thong tin KPI snapshot (UC-66, UC-67).
 * Map tay bang ham from() theo quy uoc AGENTS.md muc 3.4.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   23  field                           cac truong tra ve cho giao dien
 *   34  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class KpiSnapshotResponse {

    private Integer id;
    private Integer reporterId;
    private Short periodYear;
    private Byte periodMonth;
    private Integer actualArticles;
    private BigDecimal actualAvgScore;
    private KpiSnapshotStatus status;
    private String notes;
    private Integer finalizedBy;
    private LocalDateTime finalizedAt;

    public static KpiSnapshotResponse from(KpiSnapshot entity) {
        if (entity == null) {
            return null;
        }
        KpiSnapshotResponse res = new KpiSnapshotResponse();
        res.setId(entity.getId());
        res.setReporterId(entity.getReporterId());
        res.setPeriodYear(entity.getPeriodYear());
        res.setPeriodMonth(entity.getPeriodMonth());
        res.setActualArticles(entity.getActualArticles());
        res.setActualAvgScore(entity.getActualAvgScore());
        res.setStatus(entity.getStatus());
        res.setNotes(entity.getNotes());
        res.setFinalizedBy(entity.getFinalizedBy());
        res.setFinalizedAt(entity.getFinalizedAt());
        return res;
    }
}
