package com.swp391.kpi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Ánh xạ bảng kpi_targets trong database/schema.sql.
 * Lưu chỉ tiêu tháng của từng phóng viên do Trưởng ban thiết lập.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   30  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "kpi_targets")
@Getter
@Setter
@NoArgsConstructor
public class KpiTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // TODO(auth/admin): Doi sang @ManyToOne khi entity User hoan thanh
    private Integer reporterId;

    private Short targetYear;

    private Byte targetMonth;

    // So bai xuat ban toi thieu (>= 0)
    private Integer minPublished;

    // Diem chat luong trung binh toi thieu (thang 0-100)
    private BigDecimal minAvgScore;

    // ID cua Truong ban dat chi tieu
    // TODO(auth/admin): Doi sang @ManyToOne khi entity User hoan thanh
    private Integer setBy;
}
