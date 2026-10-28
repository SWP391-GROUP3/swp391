package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ReviewerDeviationIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy van bang cua entity ReviewerDeviationIndex (Spring Data JPA). Chi service cung luong
 * goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  findAllByOrderByPeriodDescEditorIdAsc()  tim tat ca theo order theo period desc editor id asc
 */
@Repository
public interface ReviewerDeviationRepository extends JpaRepository<ReviewerDeviationIndex, Integer> {

    List<ReviewerDeviationIndex> findAllByOrderByPeriodDescEditorIdAsc();
}
