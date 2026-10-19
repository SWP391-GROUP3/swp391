package com.swp391.kpi.repository;

import com.swp391.kpi.entity.AppealRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * MUC LUC (so dong)
 *   24  findFirstByIsActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc() - Lay cau hinh dang ap dung: is_active = 1, effective_from <= ngay kiem t…
 *   31  existsByEffectiveFrom()      - Kiem tra xem da co cau hinh bat dau tu ngay nay chua (UC-44 E2: khong d…
 *   36  findAllByOrderByEffectiveFromDesc() - Lay toan bo lich su cau hinh sap xep giam dan theo ngay hieu luc.
 */
@Repository
public interface AppealRuleConfigRepository extends JpaRepository<AppealRuleConfig, Integer> {

    /**
     * Lay cau hinh dang ap dung: is_active = 1, effective_from <= ngay kiem tra,
     * lay dong moi nhat.
     */
    Optional<AppealRuleConfig> findFirstByIsActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            LocalDate date);

    /**
     * Kiem tra xem da co cau hinh bat dau tu ngay nay chua (UC-44 E2: khong duoc
     * trung effective_from).
     */
    boolean existsByEffectiveFrom(LocalDate effectiveFrom);

    /**
     * Lay toan bo lich su cau hinh sap xep giam dan theo ngay hieu luc.
     */
    List<AppealRuleConfig> findAllByOrderByEffectiveFromDesc();
}
