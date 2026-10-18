package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ReaderInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository thao tac voi bang reader_interactions (BF-05).
 */
public interface ReaderInteractionRepository extends JpaRepository<ReaderInteraction, Integer> {

    List<ReaderInteraction> findByArticleId(Integer articleId);

    List<ReaderInteraction> findByArticleIdIn(List<Integer> articleIds);

    @Query("SELECT r.articleId, SUM(r.pageViews) FROM ReaderInteraction r WHERE r.articleId IN :articleIds GROUP BY r.articleId")
    List<Object[]> sumPageViewsByArticleIds(@Param("articleIds") List<Integer> articleIds);
}
