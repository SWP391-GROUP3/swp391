package com.swp391.kpi.repository;

import com.swp391.kpi.entity.Article;
import com.swp391.kpi.entity.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * MUC LUC (so dong)
 *   26  findMyArticles                 - UC-01 bai cua toi
 *   31  findMyArticlesByStatuses       - UC-01 loc theo trang thai
 *   38  findMyArticleStatuses          - trang chu PV: dem bai theo trang thai
 *   41  findByAuthorIdAndStatus...     - trang chu PV: bai bi tra ve
 *   57  findPublishedRows              - cho BF-04 (UC-46): moi bai da dang + ten ban, the loai
 */
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

    // Tim danh sach bai theo trang thai (BF-02 UC-29)
    List<Article> findByStatusOrderByUpdatedAtDesc(ArticleStatus status);

    // Tim danh sach bai theo trang thai va ban chuyen mon (BF-02 UC-29)
    List<Article> findByStatusAndSectionIdOrderByUpdatedAtDesc(ArticleStatus status, Integer sectionId);

    // Cho BF-04 (UC-46): moi bai PUBLISHED, dang som nhat len dau (gan han chi nhat).
    // SQL thuan (nativeQuery) vi can ten ban, ten the loai ma bang sections, genres chua co
    // entity/service (luong admin) - nhom truong (phu trach admin) dong y join tam o day.
    // TODO(admin): co SectionService, GenreService thi lay ten qua service thay vi join.
    @Query(value = "SELECT a.id AS id, a.title AS title, a.declared_nature AS declaredNature,"
            + " a.published_at AS publishedAt, a.author_id AS authorId,"
            + " a.section_id AS sectionId, s.name AS sectionName,"
            + " a.category_id AS categoryId, g.name AS categoryName"
            + " FROM articles a"
            + " JOIN sections s ON s.id = a.section_id"
            + " JOIN genres g ON g.id = a.category_id"
            + " WHERE a.status = 'PUBLISHED'"
            + " ORDER BY a.published_at ASC, a.id ASC",
            nativeQuery = true)
    List<ArticlePublishedRow> findPublishedRows();
}
