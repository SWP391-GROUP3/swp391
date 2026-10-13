package com.swp391.kpi.service;

import com.swp391.kpi.dto.RoyaltyRateCreateRequest;
import com.swp391.kpi.dto.RoyaltyRateResponse;
import com.swp391.kpi.entity.RoyaltyRate;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.repository.RoyaltyRateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiem tra luat them don gia (UC-55). Repository va SystemParameterService la "gia" (mock)
 * nen test chay khong can DB.
 */
@ExtendWith(MockitoExtension.class)
class RoyaltyRateServiceTest {

    @Mock
    private RoyaltyRateRepository royaltyRateRepository;

    @Mock
    private SystemParameterService systemParameterService;

    @InjectMocks
    private RoyaltyRateService royaltyRateService;

    // Tham so BF-04 nhu trong system_parameters (UNIT_PRICE_PERCENT cho PR [DB] them vao data.sql)
    private static Map<String, String> royaltyParams() {
        Map<String, String> params = new HashMap<>();
        params.put("NATURE_RATE_ORIGINAL", "100");
        params.put("NATURE_RATE_TRANSLATION", "70");
        params.put("NATURE_RATE_COMPILATION", "50");
        params.put("NATURE_RATE_REPOST", "30");
        params.put("MIN_FINAL_FACTOR", "1");
        params.put("ROUNDING_UNIT", "1000");
        params.put("UNIT_PRICE_PERCENT", "10");
        return params;
    }

    private static RoyaltyRate existingRate(String effectiveFrom) {
        RoyaltyRate rate = new RoyaltyRate();
        rate.setId(1);
        rate.setBaseSalary(new BigDecimal("2340000"));
        rate.setAmount(new BigDecimal("234000"));
        rate.setEffectiveFrom(LocalDate.parse(effectiveFrom));
        rate.setCreatedBy(6);
        return rate;
    }

    private static RoyaltyRateCreateRequest request(String baseSalary, String effectiveFrom) {
        RoyaltyRateCreateRequest request = new RoyaltyRateCreateRequest();
        request.setBaseSalary(new BigDecimal(baseSalary));
        request.setEffectiveFrom(LocalDate.parse(effectiveFrom));
        return request;
    }

    @Test
    void createRateCalculatesAmountFromBaseSalary() {
        when(royaltyRateRepository.findFirstByOrderByEffectiveFromDesc()).thenReturn(Optional.of(existingRate("2026-01-01")));
        when(systemParameterService.getValuesByFlow("BF-04")).thenReturn(royaltyParams());
        when(royaltyRateRepository.save(any(RoyaltyRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoyaltyRateResponse response = royaltyRateService.createRate(request("2500000", "2027-01-01"), 6);

        assertEquals(0, new BigDecimal("250000").compareTo(response.getAmount()));  // 2.500.000 x 10%
        assertEquals(LocalDate.of(2027, 1, 1), response.getEffectiveFrom());
        assertEquals(Integer.valueOf(6), response.getCreatedBy());
    }

    @Test
    void createRateRejectsDateNotAfterLatestRate() {
        when(royaltyRateRepository.findFirstByOrderByEffectiveFromDesc()).thenReturn(Optional.of(existingRate("2026-01-01")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> royaltyRateService.createRate(request("2500000", "2026-01-01"), 6));

        assertEquals("Ngày hiệu lực phải sau ngày 01/01/2026 của đơn giá mới nhất.", ex.getMessage());
        verify(royaltyRateRepository, never()).save(any());  // khong duoc luu gi
    }

    @Test
    void createFirstRateWhenNoRateExists() {
        when(royaltyRateRepository.findFirstByOrderByEffectiveFromDesc()).thenReturn(Optional.empty());
        when(systemParameterService.getValuesByFlow("BF-04")).thenReturn(royaltyParams());
        when(royaltyRateRepository.save(any(RoyaltyRate.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoyaltyRateResponse response = royaltyRateService.createRate(request("2340000", "2026-01-01"), 6);

        assertEquals(0, new BigDecimal("234000").compareTo(response.getAmount()));
    }
}
