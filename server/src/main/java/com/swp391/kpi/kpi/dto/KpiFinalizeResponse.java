package com.swp391.kpi.kpi.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO phan hoi sau khi chot ky KPI thang thanh cong (UC-66).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  field       thong tin ky, nguoi chot, thoi gian chot va thong ke ket qua
 */
@Getter
@Setter
public class KpiFinalizeResponse {

    private Short year;
    private Byte month;
    private Integer totalReporters;
    private Integer passCount;
    private Integer notPassCount;
    private Integer finalizedBy;
    private LocalDateTime finalizedAt;
    private List<KpiSnapshotResponse> snapshots = new ArrayList<>();
}
