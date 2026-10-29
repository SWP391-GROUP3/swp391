package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ScoringAttempt;
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
 *   29  findByReviewerIdAndAttemptTypeOrderByOpenedAtDesc()  tim theo reviewer id va attempt type order theo opened at desc
 *   32  countByReviewerIdAndAttemptTypeAndSubmittedAtIsNull()  dem theo reviewer id va attempt type va submitted at is null
 *   37  findLockedById()                tim locked theo id
 *   40  findByArticleIdOrderByAttemptNoAsc()  Lay toan bo lich su cac lan cham cua mot bai viet sap xep theo attempt_no
 *   43  findByArticleIdAndAttemptNo()   Lay lan cham cu the theo bai viet va so lan cham (article_id, attempt_no)
 *   46  findTopByArticleIdOrderByAttemptNoDesc()  Lay lan cham moi nhat cua mot bai viet
 *   49  findTopByAssignmentIdOrderByAttemptNoDesc()  Lay lan cham theo luot phan cong
 */
@Repository
public interface ScoringAttemptRepository extends JpaRepository<ScoringAttempt, Integer> {

    List<ScoringAttempt> findByReviewerIdAndAttemptTypeOrderByOpenedAtDesc(Integer reviewerId,
            com.swp391.kpi.entity.AttemptType attemptType);

    long countByReviewerIdAndAttemptTypeAndSubmittedAtIsNull(Integer reviewerId,
            com.swp391.kpi.entity.AttemptType attemptType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScoringAttempt s where s.id = :id")
    Optional<ScoringAttempt> findLockedById(@Param("id") Integer id);

    // Lấy toàn bộ lịch sử các lần chấm của một bài viết sắp xếp theo attempt_no
    List<ScoringAttempt> findByArticleIdOrderByAttemptNoAsc(Integer articleId);

    // Lấy lần chấm cụ thể theo bài viết và số lần chấm (article_id, attempt_no)
    Optional<ScoringAttempt> findByArticleIdAndAttemptNo(Integer articleId, Integer attemptNo);

    // Lấy lần chấm mới nhất của một bài viết
    Optional<ScoringAttempt> findTopByArticleIdOrderByAttemptNoDesc(Integer articleId);

    // Lấy lần chấm theo lượt phân công
    Optional<ScoringAttempt> findTopByAssignmentIdOrderByAttemptNoDesc(Integer assignmentId);
}
