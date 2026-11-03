package com.swp391.kpi.appeal.repository;

import com.swp391.kpi.appeal.entity.AppealCriterion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository truy van bang cua entity AppealCriterion (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   16  findAllByAppealIdOrderById()    tim tat ca theo appeal id order theo id
 */
public interface AppealCriterionRepository extends JpaRepository<AppealCriterion, Integer> {

    List<AppealCriterion> findAllByAppealIdOrderById(Integer appealId);
}
