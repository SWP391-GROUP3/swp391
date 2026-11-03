package com.swp391.kpi.kpi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Anh xa bang kpi_snapshots trong database/schema.sql.
 * Ket qua KPI thang cua phong vien.
 * DRAFT = so tam, tinh lai duoc; FINALIZED = da chot, bat bien (BR-KPI-06).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   35  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "kpi_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class KpiSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // ID phong vien duoc danh gia KPI
    private Integer reporterId;

    private Short periodYear;

    private Byte periodMonth;

    // So bai xuat ban quy doi theo ty le dong tac gia (>= 0)
    private Integer actualArticles;

    // Diem chat luong trung binh (thang 0-100), NULL khi chua co bai co diem
    private BigDecimal actualAvgScore;

    @Enumerated(EnumType.STRING)
    private KpiSnapshotStatus status;

    @Nationalized
    private String notes;

    // ID nguoi chot KPI (Truong ban). NULL khi status = DRAFT
    private Integer finalizedBy;

    // Thoi diem chot KPI. NULL khi status = DRAFT
    private LocalDateTime finalizedAt;
}
