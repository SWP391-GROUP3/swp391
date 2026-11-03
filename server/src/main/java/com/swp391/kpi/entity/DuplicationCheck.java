package com.swp391.kpi.entity;

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
 * 1 lan kiem trung noi dung tren 1 ban nop (bang duplication_checks, BR-IN-04).
 * Iter 3: tinh theo cum am tiet (DuplicationShingleCalculator), cac bai bi trung luu o duplication_matches.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   34  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "duplication_checks")
@Getter
@Setter
@NoArgsConstructor
public class DuplicationCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer articleId;

    private Integer versionId;              // ban nop (article_versions) duoc kiem

    private BigDecimal maxOverlapPercent;   // % trung cao nhat, 0-100

    @Enumerated(EnumType.STRING)
    private DuplicationResult result;

    @Nationalized   // cot NVARCHAR, giai trinh cua PV khi NEED_EXPLAIN
    private String explanation;

    private LocalDateTime checkedAt;
}
