package com.swp391.kpi.assessment.repository;

import com.swp391.kpi.assessment.entity.Criteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy van bang cua entity Criteria (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  findByCriteriaSetVersionIdOrderBySortOrderAsc()  Lay danh sach tieu chi thuoc ve mot version bo tieu chi sap xep theo sort_order
 */
@Repository
public interface CriteriaRepository extends JpaRepository<Criteria, Integer> {

    // Lấy danh sách tiêu chí thuộc về một version bộ tiêu chí sắp xếp theo sort_order
    List<Criteria> findByCriteriaSetVersionIdOrderBySortOrderAsc(Integer criteriaSetVersionId);
}
