package com.swp391.kpi.royalty.entity;

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

/**
 * Don gia nhuan but (bang royalty_rates, UC-55).
 * Doi don gia = THEM dong moi voi ngay hieu luc moi, KHONG sua dong cu:
 * bai dang ngay nao dung don gia co effective_from lon nhat ma <= ngay do (BR-RY-03).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   32  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "royalty_rates")
@Getter
@Setter
@NoArgsConstructor
public class RoyaltyRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private BigDecimal baseSalary;    // luong co so, DECIMAL(18,0)

    private BigDecimal amount;        // don gia cua 1 he so = 10% luong co so, DECIMAL(18,0)

    private LocalDate effectiveFrom;  // ngay bat dau ap dung, UNIQUE trong bang

    // FK toi users(id). Chua co entity User (luong admin) nen tam de Integer (AGENTS.md 3.3),
    // co entity User thi doi sang @ManyToOne(fetch = FetchType.LAZY).
    private Integer createdBy;
}
