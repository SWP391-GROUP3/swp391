package com.swp391.kpi.repository;

import com.swp391.kpi.entity.Criteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MUC LUC (so dong)
 *   17  findByCriteriaSetVersionIdOrderBySortOrderAsc() - Lấy danh sách tiêu chí thuộc về một version bộ tiêu chí sắp xếp theo so…
 */
@Repository
public interface CriteriaRepository extends JpaRepository<Criteria, Integer> {

    // Lấy danh sách tiêu chí thuộc về một version bộ tiêu chí sắp xếp theo sort_order
    List<Criteria> findByCriteriaSetVersionIdOrderBySortOrderAsc(Integer criteriaSetVersionId);
}
