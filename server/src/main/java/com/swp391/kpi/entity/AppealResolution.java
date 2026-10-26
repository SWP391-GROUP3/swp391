package com.swp391.kpi.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MUC LUC (so dong)
 *   17  field                        - cac cot cua bang (khop schema.sql)
 */
@Entity @Table(name = "appeal_resolutions")
@Getter @Setter @NoArgsConstructor
public class AppealResolution {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer appealId;
    private Integer effectiveAttemptId;
    @Enumerated(EnumType.STRING)
    private AppealResolutionRule ruleApplied;
    private Integer finalizedBy;
    @Column(precision = 5, scale = 2)
    private BigDecimal effectiveScore;
    private LocalDateTime finalizedAt;
}
