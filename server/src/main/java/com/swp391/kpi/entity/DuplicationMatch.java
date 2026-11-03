package com.swp391.kpi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

/**
 * 1 bai bi trung trong 1 lan kiem trung noi dung (bang duplication_matches, UC-06, B4).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   30  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "duplication_matches")
@Getter
@Setter
@NoArgsConstructor
public class DuplicationMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer checkId;             // lan kiem (duplication_checks)

    private Integer matchedArticleId;    // bai bi trung

    private BigDecimal overlapPercent;   // % trung voi bai nay, 0-100

    @Nationalized   // cot NVARCHAR(MAX): cac doan trung, dang JSON mang chuoi (B4)
    private String passages;
}
