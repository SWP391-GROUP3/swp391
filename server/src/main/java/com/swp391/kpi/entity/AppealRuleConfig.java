package com.swp391.kpi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cau hinh luat khieu nai theo ngay hieu luc (bang appeal_rule_configs).
 * Quy tac: Duyen dong is_active = 1 co effective_from lon nhat ma <= ngay hien tai.
 */
@Entity
@Table(name = "appeal_rule_configs")
@Getter
@Setter
@NoArgsConstructor
public class AppealRuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer appealWindowDays;

    private Integer maxAppealsPerArticle;

    private BigDecimal varianceThreshold;

    private LocalDate effectiveFrom;

    private Boolean isActive = true;

    // FK toi bang users(id). Tam de Integer theo AGENTS.md vi User entity thuoc luong admin
    @Column(name = "updated_by")
    private Integer updatedBy;
}
