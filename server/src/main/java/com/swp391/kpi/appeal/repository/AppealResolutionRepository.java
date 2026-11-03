package com.swp391.kpi.appeal.repository;
import com.swp391.kpi.appeal.entity.AppealResolution;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Repository truy van bang cua entity AppealResolution (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   11  findByAppealId()                tim theo appeal id
 */
public interface AppealResolutionRepository extends JpaRepository<AppealResolution, Integer> {
    java.util.Optional<AppealResolution> findByAppealId(Integer appealId);
}
