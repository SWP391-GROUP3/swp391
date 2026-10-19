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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MUC LUC (so dong)
 *   29  field                        - cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "reviewer_deviation_indices")
@Getter
@Setter
@NoArgsConstructor
public class ReviewerDeviationIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK toi users(id). Tam de Integer vi User entity thuoc luong admin.
    private Integer editorId;

    private LocalDate period;

    private BigDecimal avgDeviation;

    private Integer appealCount;

    private BigDecimal overturnRate;

    private Integer workloadCount;

    private LocalDateTime lastUpdated;
}
