package com.swp391.kpi.dto;

import com.swp391.kpi.entity.RoyaltyRate;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 1 dong trong bang lich su don gia tra ve cho man Unit Price List (UC-55).
 */
@Getter
@Setter
public class RoyaltyRateResponse {

    private Integer id;
    private BigDecimal baseSalary;
    private BigDecimal amount;
    private LocalDate effectiveFrom;
    private Integer createdBy;

    // Map tay entity -> DTO (AGENTS.md muc 3.4)
    public static RoyaltyRateResponse from(RoyaltyRate rate) {
        RoyaltyRateResponse response = new RoyaltyRateResponse();
        response.setId(rate.getId());
        response.setBaseSalary(rate.getBaseSalary());
        response.setAmount(rate.getAmount());
        response.setEffectiveFrom(rate.getEffectiveFrom());
        response.setCreatedBy(rate.getCreatedBy());
        return response;
    }
}
