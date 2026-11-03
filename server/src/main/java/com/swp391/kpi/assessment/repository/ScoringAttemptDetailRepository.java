package com.swp391.kpi.assessment.repository;

import com.swp391.kpi.assessment.entity.ScoringAttemptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy van bang cua entity ScoringAttemptDetail (Spring Data JPA). Chi service cung luong
 * goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  findByScoringAttemptId()        Lay toan bo diem chi tiet cua mot lan cham (UC-30, UC-34)
 *   25  findByScoringAttemptIdAndCriterionId()  Lay diem cua mot tieu chi cu the trong lan cham
 */
@Repository
public interface ScoringAttemptDetailRepository extends JpaRepository<ScoringAttemptDetail, Integer> {

    // Lấy toàn bộ điểm chi tiết của một lần chấm (UC-30, UC-34)
    List<ScoringAttemptDetail> findByScoringAttemptId(Integer scoringAttemptId);

    // Lấy điểm của một tiêu chí cụ thể trong lần chấm
    Optional<ScoringAttemptDetail> findByScoringAttemptIdAndCriterionId(Integer scoringAttemptId, Integer criterionId);
}
