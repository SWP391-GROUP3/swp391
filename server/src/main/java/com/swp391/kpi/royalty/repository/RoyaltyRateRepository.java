package com.swp391.kpi.royalty.repository;

import com.swp391.kpi.royalty.entity.RoyaltyRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Doc/ghi bang royalty_rates (don gia nhuan but, UC-55).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  findAllByOrderByEffectiveFromDesc()  Lich su don gia, ngay hieu luc moi nhat len dau (man Unit Price List)
 *   24  findFirstByOrderByEffectiveFromDesc()  Don gia co ngay hieu luc moi nhat: de kiem ngay hieu luc cua don gia sap them
 *   28  findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc()  Don gia hieu luc tai 1 ngay = dong co effective_from lon nhat ma <= ngay do (GBR-08, BR-RY-03)
 */
public interface RoyaltyRateRepository extends JpaRepository<RoyaltyRate, Integer> {

    // Lich su don gia, ngay hieu luc moi nhat len dau (man Unit Price List)
    List<RoyaltyRate> findAllByOrderByEffectiveFromDesc();

    // Don gia co ngay hieu luc moi nhat: de kiem ngay hieu luc cua don gia sap them
    Optional<RoyaltyRate> findFirstByOrderByEffectiveFromDesc();

    // Don gia hieu luc tai 1 ngay = dong co effective_from lon nhat ma <= ngay do (GBR-08, BR-RY-03).
    // UC-47 dung voi ngay dang bai.
    Optional<RoyaltyRate> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate date);
}
