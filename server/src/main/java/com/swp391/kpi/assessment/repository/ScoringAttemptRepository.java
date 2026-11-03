package com.swp391.kpi.assessment.repository;

import com.swp391.kpi.assessment.entity.ScoringAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository truy van bang cua entity ScoringAttempt (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   30  findByReviewerIdAndAttemptTypeOrderByOpenedAtDesc()  tim theo reviewer id va attempt type order theo opened at desc
 *   33  countByReviewerIdAndAttemptTypeAndSubmittedAtIsNull()  dem theo reviewer id va attempt type va submitted at is null
 *   38  findLockedById()                tim locked theo id
 *   41  findByArticleIdOrderByAttemptNoAsc()  Lay toan bo lich su cac lan cham cua mot bai viet sap xep theo attempt_no
 *   44  findByArticleIdAndAttemptNo()   Lay lan cham cu the theo bai viet va so lan cham (article_id, attempt_no)
 *   47  findTopByArticleIdOrderByAttemptNoDesc()  Lay lan cham moi nhat cua mot bai viet (moi loai, ke ca APPEAL) - chi dung de lay so attempt_no tiep theo
 *   50  findTopByArticleIdAndAttemptTypeNotOrderByAttemptNoDesc()  M-A2: lan cham moi nhat cua BF-02 (bo qua loai truyen vao, vd APPEAL do BF-03 ghi)
 *   54  findTopByAssignmentIdOrderByAttemptNoDesc()  Lay lan cham theo luot phan cong
 */
@Repository
public interface ScoringAttemptRepository extends JpaRepository<ScoringAttempt, Integer> {

    List<ScoringAttempt> findByReviewerIdAndAttemptTypeOrderByOpenedAtDesc(Integer reviewerId,
            com.swp391.kpi.assessment.entity.AttemptType attemptType);

    long countByReviewerIdAndAttemptTypeAndSubmittedAtIsNull(Integer reviewerId,
            com.swp391.kpi.assessment.entity.AttemptType attemptType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScoringAttempt s where s.id = :id")
    Optional<ScoringAttempt> findLockedById(@Param("id") Integer id);

    // Lấy toàn bộ lịch sử các lần chấm của một bài viết sắp xếp theo attempt_no
    List<ScoringAttempt> findByArticleIdOrderByAttemptNoAsc(Integer articleId);

    // Lấy lần chấm cụ thể theo bài viết và số lần chấm (article_id, attempt_no)
    Optional<ScoringAttempt> findByArticleIdAndAttemptNo(Integer articleId, Integer attemptNo);

    // Lấy lần chấm mới nhất của một bài viết (mọi loại, kể cả APPEAL) - chỉ dùng để lấy số attempt_no tiếp theo
    Optional<ScoringAttempt> findTopByArticleIdOrderByAttemptNoDesc(Integer articleId);

    // M-A2: lần chấm mới nhất của BF-02 (bỏ qua loại truyền vào, vd APPEAL do BF-03 ghi)
    Optional<ScoringAttempt> findTopByArticleIdAndAttemptTypeNotOrderByAttemptNoDesc(Integer articleId,
            com.swp391.kpi.assessment.entity.AttemptType attemptType);

    // Lấy lần chấm theo lượt phân công
    Optional<ScoringAttempt> findTopByAssignmentIdOrderByAttemptNoDesc(Integer assignmentId);
}
