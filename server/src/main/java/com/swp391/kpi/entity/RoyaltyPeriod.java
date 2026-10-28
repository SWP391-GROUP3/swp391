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

import java.time.LocalDateTime;

/**
 * Ky nhuan but theo thang (bang royalty_periods, BR-RY-07):
 * OPEN -> CLOSED (Thu ky chot) -> LOCKED (Ke toan khoa) -> PAID (da chi het).
 * Moi thang chi co 1 ky: UNIQUE(period_year, period_month).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   34  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "royalty_periods")
@Getter
@Setter
@NoArgsConstructor
public class RoyaltyPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Short periodYear;     // SMALLINT

    private Byte periodMonth;     // TINYINT, 1-12

    @Enumerated(EnumType.STRING)  // luu chu "OPEN"... xuong DB, khong luu so thu tu
    private RoyaltyPeriodStatus status;

    // FK toi users(id). Chua co entity User nen tam de Integer (AGENTS.md 3.3)
    private Integer lockedBy;     // Ke toan khoa ky, NULL khi chua khoa

    private LocalDateTime lockedAt;

    // FK toi users(id): Ke toan tra ky ve gan nhat
    private Integer returnedBy;

    @Nationalized                 // cot NVARCHAR
    private String returnNote;    // ly do tra ve, bat buoc khi tra ve

    private LocalDateTime paidAt;
}
