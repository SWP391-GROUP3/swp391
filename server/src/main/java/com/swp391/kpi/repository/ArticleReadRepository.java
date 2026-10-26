package com.swp391.kpi.repository;

import com.swp391.kpi.dto.ArticleReadResponse;
import com.swp391.kpi.dto.ArticleVersionReadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * MUC LUC (so dong)
 *   18  field                        - cac truong du lieu
 *   20  findPublishedByAuthorId()
 *   28  findById()
 */
@Repository
@RequiredArgsConstructor
public class ArticleReadRepository {
    private final JdbcTemplate jdbcTemplate;

    public boolean isAuthor(Integer articleId, Integer userId) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM articles a WHERE a.id=? AND (a.author_id=? OR EXISTS
                  (SELECT 1 FROM article_authors au WHERE au.article_id=a.id AND au.user_id=?))
                """, Integer.class, articleId, userId, userId) > 0;
    }

    public List<ArticleVersionReadResponse> findVersion(Integer articleId, Integer versionId) {
        return jdbcTemplate.query("SELECT id,article_id,title,content FROM article_versions WHERE id=? AND article_id=?",
                (row, index) -> new ArticleVersionReadResponse(row.getInt("id"), row.getInt("article_id"),
                        row.getString("title"), row.getString("content")), versionId, articleId);
    }

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
