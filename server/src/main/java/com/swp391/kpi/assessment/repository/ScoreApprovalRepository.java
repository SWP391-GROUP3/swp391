package com.swp391.kpi.assessment.repository;

import com.swp391.kpi.assessment.entity.ApprovalDecision;
import com.swp391.kpi.assessment.entity.ScoreApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy van bang cua entity ScoreApproval (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  findByScoringAttemptId()        Lay ket qua duyet diem cua mot lan cham
 *   25  findByScoringAttemptArticleIdAndDecisionAndAnnouncedAtIsNotNullOrderByAnnouncedAtDesc()  M-D2: cac lan duyet cua 1 bai theo quyet dinh, da cong bo diem, cong bo moi nhat truoc
 */
@Repository
public interface ScoreApprovalRepository extends JpaRepository<ScoreApproval, Integer> {

    // Lấy kết quả duyệt điểm của một lần chấm
    Optional<ScoreApproval> findByScoringAttemptId(Integer scoringAttemptId);

    // M-D2: cac lan duyet cua 1 bai theo quyet dinh, da cong bo diem, cong bo moi nhat truoc
    List<ScoreApproval> findByScoringAttemptArticleIdAndDecisionAndAnnouncedAtIsNotNullOrderByAnnouncedAtDesc(
            Integer articleId, ApprovalDecision decision);
}
