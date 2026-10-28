package com.swp391.kpi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/**
 * Entity anh xa bang appeal_criteria trong schema.sql. Chi dung trong service, khong tra thang ra API.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   32  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "appeal_criteria")
@Getter
@Setter
@NoArgsConstructor
public class AppealCriterion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appeal_id", nullable = false)
    private Appeal appeal;

    private Integer criterionId;

    @Nationalized
    @Column(length = 1000)
    private String reason;
}
