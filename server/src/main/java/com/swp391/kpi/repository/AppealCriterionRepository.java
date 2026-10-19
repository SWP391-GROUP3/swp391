package com.swp391.kpi.repository;

import com.swp391.kpi.entity.AppealCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppealCriterionRepository extends JpaRepository<AppealCriterion, Integer> {

    List<AppealCriterion> findAllByAppealIdOrderById(Integer appealId);
}
