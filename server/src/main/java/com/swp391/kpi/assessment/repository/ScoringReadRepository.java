package com.swp391.kpi.assessment.repository;

import com.swp391.kpi.assessment.dto.ScoringCriterionResponse;
import com.swp391.kpi.assessment.dto.ScoringReadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository truy van bang cua entity ? (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   26  field                           cac truong du lieu
 *   28  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *  ---- Ham public (controller / luong khac goi) ----
 *   35  findSubmitted()                 tim submitted
 *   39  findSubmittedByArticleId()      tim submitted theo article id
 *   51  findCriteria()                  tim criteria
 *  ---- Ham phu (private, chi dung trong file) ----
 *   43  toResponse()                    doi sang response
 */
@Repository
@RequiredArgsConstructor
public class ScoringReadRepository {
    private final JdbcTemplate jdbcTemplate;

    private static final String SUBMITTED_SQL = """
            SELECT a.id, a.article_id, a.reviewer_id, a.attempt_type, a.total_score, a.submitted_at, a.appeal_id,
                   CASE WHEN p.decision = 'APPROVED' THEN p.announced_at END AS announced_at
            FROM scoring_attempts a LEFT JOIN score_approvals p ON p.scoring_attempt_id = a.id
            WHERE a.submitted_at IS NOT NULL
            """;

    public List<ScoringReadResponse> findSubmitted() {
        return jdbcTemplate.query(SUBMITTED_SQL, (row, index) -> toResponse(row));
    }

    public List<ScoringReadResponse> findSubmittedByArticleId(Integer articleId) {
        return jdbcTemplate.query(SUBMITTED_SQL + " AND a.article_id = ?", (row, index) -> toResponse(row), articleId);
    }

    private ScoringReadResponse toResponse(java.sql.ResultSet row) throws java.sql.SQLException {
        return new ScoringReadResponse(row.getInt("id"), row.getInt("article_id"),
                row.getInt("reviewer_id"), row.getString("attempt_type"), row.getBigDecimal("total_score"),
                row.getObject("submitted_at", java.time.LocalDateTime.class),
                row.getObject("announced_at", java.time.LocalDateTime.class),
                row.getObject("appeal_id", Integer.class));
    }

    public List<ScoringCriterionResponse> findCriteria(Integer attemptId) {
        return jdbcTemplate.query("""
                SELECT c.id, c.name, d.score, d.comment
                FROM scoring_attempt_details d JOIN criteria c ON c.id = d.criterion_id
                JOIN scoring_attempts a ON a.id = d.scoring_attempt_id
                WHERE a.id = ? AND c.criteria_set_version_id = a.criteria_set_version_id
                ORDER BY c.id
                """, (row, index) -> new ScoringCriterionResponse(row.getInt("id"), row.getString("name"),
                row.getBigDecimal("score"), row.getString("comment")), attemptId);
    }
}
