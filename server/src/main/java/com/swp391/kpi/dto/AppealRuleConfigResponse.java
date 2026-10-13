package com.swp391.kpi.dto;

import com.swp391.kpi.entity.AppealRuleConfig;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AppealRuleConfigResponse {

    private Integer id;
    private Integer appealWindowDays;
    private Integer maxAppealsPerArticle;
    private BigDecimal varianceThreshold;
    private LocalDate effectiveFrom;
    private Boolean isActive;
    private Integer updatedBy;

    // Map thu cong bang static method theo quy uoc AGENTS.md muc 3.4
    public static AppealRuleConfigResponse from(AppealRuleConfig entity) {
        if (entity == null) {
            return null;
        }
        AppealRuleConfigResponse dto = new AppealRuleConfigResponse();
        dto.setId(entity.getId());
        dto.setAppealWindowDays(entity.getAppealWindowDays());
        dto.setMaxAppealsPerArticle(entity.getMaxAppealsPerArticle());
        dto.setVarianceThreshold(entity.getVarianceThreshold());
        dto.setEffectiveFrom(entity.getEffectiveFrom());
        dto.setIsActive(entity.getIsActive());
        dto.setUpdatedBy(entity.getUpdatedBy());
        return dto;
    }
}
