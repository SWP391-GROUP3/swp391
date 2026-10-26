package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ReaderInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository thao tac voi bang reader_interactions (BF-05).
 *
 * MUC LUC (so dong)
 *   20  findByArticleId()
 *   22  findByArticleIdIn()
 *   24  findByArticleIdAndRecordDate()
 *   27  sumPageViewsByArticleIds()
 */
public interface ReaderInteractionRepository extends JpaRepository<ReaderInteraction, Integer> {

    List<ReaderInteraction> findByArticleId(Integer articleId);

    List<ReaderInteraction> findByArticleIdIn(List<Integer> articleIds);

    Optional<ReaderInteraction> findByArticleIdAndRecordDate(Integer articleId, LocalDate recordDate);

    @Query("SELECT r.articleId, SUM(r.pageViews) FROM ReaderInteraction r WHERE r.articleId IN :articleIds GROUP BY r.articleId")
    List<Object[]> sumPageViewsByArticleIds(@Param("articleIds") List<Integer> articleIds);
}
