package com.swp391.kpi.repository;

import com.swp391.kpi.entity.RoyaltyRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Doc/ghi bang royalty_rates (don gia nhuan but, UC-55).
 */
public interface RoyaltyRateRepository extends JpaRepository<RoyaltyRate, Integer> {

    // Lich su don gia, ngay hieu luc moi nhat len dau (man Unit Price List)
    List<RoyaltyRate> findAllByOrderByEffectiveFromDesc();

    // Don gia co ngay hieu luc moi nhat: de kiem ngay hieu luc cua don gia sap them
    Optional<RoyaltyRate> findFirstByOrderByEffectiveFromDesc();
}
