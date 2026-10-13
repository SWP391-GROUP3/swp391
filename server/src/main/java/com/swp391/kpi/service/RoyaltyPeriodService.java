package com.swp391.kpi.service;

import com.swp391.kpi.dto.RoyaltyPeriodResponse;
import com.swp391.kpi.entity.RoyaltyPeriod;
import com.swp391.kpi.repository.RoyaltyPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Xem cac ky nhuan but (UC-49, Thu ky va Ke toan).
 */
@Service
@RequiredArgsConstructor
public class RoyaltyPeriodService {

    private final RoyaltyPeriodRepository royaltyPeriodRepository;

    // Cac ky moi nhat len dau, kem tong tien va so bai da cham.
    // Moi ky chay them 2 cau dem: so ky it (12 ky/nam) nen chap nhan duoc.
    @Transactional(readOnly = true)
    public List<RoyaltyPeriodResponse> getAllPeriods() {
        List<RoyaltyPeriodResponse> result = new ArrayList<>();
        for (RoyaltyPeriod period : royaltyPeriodRepository.findAllByOrderByPeriodYearDescPeriodMonthDesc()) {
            BigDecimal totalAmount = royaltyPeriodRepository.sumLineAmountByPeriodId(period.getId());
            Integer gradedArticleCount = royaltyPeriodRepository.countGradedArticlesByPeriodId(period.getId());
            result.add(RoyaltyPeriodResponse.from(period, totalAmount, gradedArticleCount));
        }
        return result;
    }
}
