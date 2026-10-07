package com.swp391.kpi.royalty.repository;

import com.swp391.kpi.royalty.entity.RoyaltyPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoyaltyPeriodRepository extends JpaRepository<RoyaltyPeriod, Integer> {

    // Ky moi nhat len dau
    List<RoyaltyPeriod> findAllByOrderByPeriodYearDescPeriodMonthDesc();
}
