package com.swp391.kpi.dto;

import com.swp391.kpi.entity.KpiSnapshotStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Thong tin chi tiet ket qua va tien do KPI cua 1 phong vien (UC-62, UC-60).
 *
 * MUC LUC (so dong)
 *   24  field                        - cac truong tra ve cho giao dien
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReporterKpiDetailResponse {

    private Integer reporterId;
    private String reporterName;
    private String reporterEmail;
    private Short year;
    private Byte month;

    // Chi tieu giao (kpi_targets)
    private Integer minPublished;
    private BigDecimal minAvgScore;

    // Thuc te tich luy (kpi_snapshots hoac tinh truc tiep)
    private Integer actualArticles;
    private BigDecimal exactActualArticles;
    private BigDecimal actualAvgScore;
    private Integer totalViews;

    // Phan tram tien do so bai
    private BigDecimal articleProgressPercent;

    // Danh gia PASS / NOT_PASS (BR-KPI-05)
    private String projectedStatus;

    // Trang thai snapshot (DRAFT hoac FINALIZED)
    private KpiSnapshotStatus snapshotStatus;

    // Nhan xet danh gia cua Truong ban
    private String notes;

    // Danh sach chi tiet cac bai viet da xuat ban cua phong vien trong ky
    private List<ReporterKpiArticleItemResponse> articles;
}
