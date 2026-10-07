package com.swp391.kpi.royalty.service;

import com.swp391.kpi.royalty.dto.RoyaltyPeriodResponse;
import com.swp391.kpi.royalty.entity.RoyaltyPeriod;
import com.swp391.kpi.royalty.repository.RoyaltyPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoyaltyPeriodService {

    private final RoyaltyPeriodRepository periodRepository;

    @Transactional(readOnly = true)
    public List<RoyaltyPeriodResponse> getAll() {
        List<RoyaltyPeriod> periods = periodRepository.findAllByOrderByPeriodYearDescPeriodMonthDesc();

        List<RoyaltyPeriodResponse> result = new ArrayList<>();
        for (RoyaltyPeriod period : periods) {
            result.add(RoyaltyPeriodResponse.from(period));
        }
        return result;
    }
}
