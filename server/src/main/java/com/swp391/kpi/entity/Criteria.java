package com.swp391.kpi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Một tiêu chí chấm thành phần trong phiên bản bộ tiêu chí.
 * Bảng: criteria
 *
 * MUC LUC (so dong)
 *   26  field                        - cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "criteria")
@Getter
@Setter
@NoArgsConstructor
public class Criteria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criteria_set_version_id", nullable = false)
    private CriteriaSetVersion criteriaSetVersion;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // Trọng số phần trăm (%) từ 0.01 đến 100.00
    @Column(name = "weight", nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 1;
}
