package com.swp391.kpi.service;

import com.swp391.kpi.dto.RoyaltyPeriodResponse;
import com.swp391.kpi.entity.RoyaltyPeriod;
import com.swp391.kpi.entity.RoyaltyPeriodStatus;
import com.swp391.kpi.repository.RoyaltyPeriodRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Kiem tra danh sach ky nhuan but (UC-49), khong can DB.
 */
@ExtendWith(MockitoExtension.class)
class RoyaltyPeriodServiceTest {

    @Mock
    private RoyaltyPeriodRepository royaltyPeriodRepository;

    @InjectMocks
    private RoyaltyPeriodService royaltyPeriodService;

    @Test
    void getAllPeriodsAddsTotalAmountAndGradedArticleCount() {
        RoyaltyPeriod october = new RoyaltyPeriod();
        october.setId(3);
        october.setPeriodYear((short) 2026);
        october.setPeriodMonth((byte) 10);
        october.setStatus(RoyaltyPeriodStatus.OPEN);
        when(royaltyPeriodRepository.findAllByOrderByPeriodYearDescPeriodMonthDesc()).thenReturn(List.of(october));
        when(royaltyPeriodRepository.sumLineAmountByPeriodId(3)).thenReturn(new BigDecimal("1233000"));
        when(royaltyPeriodRepository.countGradedArticlesByPeriodId(3)).thenReturn(2);

        List<RoyaltyPeriodResponse> result = royaltyPeriodService.getAllPeriods();

        assertEquals(1, result.size());
        RoyaltyPeriodResponse period = result.get(0);
        assertEquals(RoyaltyPeriodStatus.OPEN, period.getStatus());
        assertEquals(Byte.valueOf((byte) 10), period.getPeriodMonth());
        assertEquals(0, new BigDecimal("1233000").compareTo(period.getTotalAmount()));
        assertEquals(Integer.valueOf(2), period.getGradedArticleCount());
    }
}
