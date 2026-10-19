package com.swp391.kpi.repository;

import com.swp391.kpi.entity.AppealCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MUC LUC (so dong)
 *   14  findAllByAppealIdOrderById()
 */
public interface AppealCriterionRepository extends JpaRepository<AppealCriterion, Integer> {

    List<AppealCriterion> findAllByAppealIdOrderById(Integer appealId);
}
