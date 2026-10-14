package com.swp391.kpi.repository;

import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Integer> {

    // Bai ma nguoi dung la tac gia chinh hoac dong tac gia (UC-01)
    @Query("SELECT a FROM Article a WHERE a.authorId = :userId"
            + " OR a.id IN (SELECT aa.articleId FROM ArticleAuthor aa WHERE aa.userId = :userId)")
    Page<Article> findMyArticles(@Param("userId") Integer userId, Pageable pageable);

    // Nhu tren nhung chi lay bai co trang thai nam trong danh sach statuses
    @Query("SELECT a FROM Article a WHERE a.status IN :statuses AND (a.authorId = :userId"
            + " OR a.id IN (SELECT aa.articleId FROM ArticleAuthor aa WHERE aa.userId = :userId))")
    Page<Article> findMyArticlesByStatuses(@Param("userId") Integer userId,
                                           @Param("statuses") List<ArticleStatus> statuses,
                                           Pageable pageable);

    // Trang thai cua moi bai cua nguoi dung, de dem so bai theo trang thai (trang chu PV)
    @Query("SELECT a.status FROM Article a WHERE a.authorId = :userId"
            + " OR a.id IN (SELECT aa.articleId FROM ArticleAuthor aa WHERE aa.userId = :userId)")
    List<ArticleStatus> findMyArticleStatuses(@Param("userId") Integer userId);

    // Bai cua tac gia chinh dang o 1 trang thai (vd bai bi tra ve REVISION_REQUESTED)
    List<Article> findByAuthorIdAndStatusOrderByUpdatedAtDesc(Integer authorId, ArticleStatus status);
}
