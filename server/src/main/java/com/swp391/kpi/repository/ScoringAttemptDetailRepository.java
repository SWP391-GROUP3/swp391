package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ScoringAttemptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoringAttemptDetailRepository extends JpaRepository<ScoringAttemptDetail, Integer> {

    // Lấy toàn bộ điểm chi tiết của một lần chấm (UC-30, UC-34)
    List<ScoringAttemptDetail> findByScoringAttemptId(Integer scoringAttemptId);

    // Lấy điểm của một tiêu chí cụ thể trong lần chấm
    Optional<ScoringAttemptDetail> findByScoringAttemptIdAndCriterionId(Integer scoringAttemptId, Integer criterionId);
}
