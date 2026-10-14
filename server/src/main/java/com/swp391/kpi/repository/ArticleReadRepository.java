package com.swp391.kpi.repository;

import com.swp391.kpi.dto.ArticleReadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ArticleReadRepository {
    private final JdbcTemplate jdbcTemplate;

    public List<ArticleReadResponse> findPublishedByAuthorId(Integer authorId) {
        return jdbcTemplate.query("""
                SELECT id, author_id, section_id, title, status FROM articles
                WHERE author_id = ? AND status IN ('SCORE_PUBLISHED','PUBLISHED')
                """, (row, index) -> new ArticleReadResponse(row.getInt("id"), row.getInt("author_id"),
                row.getInt("section_id"), row.getString("title"), row.getString("status")), authorId);
    }

    public List<ArticleReadResponse> findById(Integer id) {
        return jdbcTemplate.query("SELECT id, author_id, section_id, title, status FROM articles WHERE id = ?",
                (row, index) -> new ArticleReadResponse(row.getInt("id"), row.getInt("author_id"),
                        row.getInt("section_id"), row.getString("title"), row.getString("status")), id);
    }
}
