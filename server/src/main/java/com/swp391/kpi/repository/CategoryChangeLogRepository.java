package com.swp391.kpi.repository;

import com.swp391.kpi.entity.CategoryChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy van bang cua entity CategoryChangeLog (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  findByAssignmentIdOrderByChangedAtDesc()  Lay lich su doi the loai cua mot luot phan cong (UC-23, BR-QA-07)
 */
@Repository
public interface CategoryChangeLogRepository extends JpaRepository<CategoryChangeLog, Integer> {

    // Lấy lịch sử đổi thể loại của một lượt phân công (UC-23, BR-QA-07)
    List<CategoryChangeLog> findByAssignmentIdOrderByChangedAtDesc(Integer assignmentId);
}
