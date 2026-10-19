package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ScoreApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScoreApprovalRepository extends JpaRepository<ScoreApproval, Integer> {

    // Lấy kết quả duyệt điểm của một lần chấm
    Optional<ScoreApproval> findByScoringAttemptId(Integer scoringAttemptId);
}
