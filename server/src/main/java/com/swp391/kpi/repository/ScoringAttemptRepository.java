package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ScoringAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MUC LUC (so dong)
 *   21  findByArticleIdOrderByAttemptNoAsc() - Lấy toàn bộ lịch sử các lần chấm của một bài viết sắp xếp theo attempt_…
 *   24  findByArticleIdAndAttemptNo() - Lấy lần chấm cụ thể theo bài viết và số lần chấm (article_id, attempt_n…
 *   27  findTopByArticleIdOrderByAttemptNoDesc() - Lấy lần chấm mới nhất của một bài viết
 *   30  findTopByAssignmentIdOrderByAttemptNoDesc() - Lấy lần chấm theo lượt phân công
 */
@Repository
public interface ScoringAttemptRepository extends JpaRepository<ScoringAttempt, Integer> {

    // Lấy toàn bộ lịch sử các lần chấm của một bài viết sắp xếp theo attempt_no
    List<ScoringAttempt> findByArticleIdOrderByAttemptNoAsc(Integer articleId);

    // Lấy lần chấm cụ thể theo bài viết và số lần chấm (article_id, attempt_no)
    Optional<ScoringAttempt> findByArticleIdAndAttemptNo(Integer articleId, Integer attemptNo);

    // Lấy lần chấm mới nhất của một bài viết
    Optional<ScoringAttempt> findTopByArticleIdOrderByAttemptNoDesc(Integer articleId);

    // Lấy lần chấm theo lượt phân công
    Optional<ScoringAttempt> findTopByAssignmentIdOrderByAttemptNoDesc(Integer assignmentId);
}
