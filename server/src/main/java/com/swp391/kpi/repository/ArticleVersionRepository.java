package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.entity.ArticleVersion;
import com.swp391.kpi.entity.ArticleVersionKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy van bang cua entity ArticleVersion (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   25  findFirstByArticleIdAndKindOrderByVersionNoDesc()  Ban moi nhat cua bai theo loai
 *   28  findFirstByArticleIdOrderByVersionNoDesc()  Ban moi nhat cua bai, moi loai (de lay so version_no tiep theo khi nop)
 *   35  findLatestOfOtherArticles()     B3 (UC-06): ban nop (kind) moi nhat cua moi bai KHAC da nop (trang thai khac DRAFT) de kiem trung noi dung
 */
public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, Integer> {

    // Ban moi nhat cua bai theo loai. Vd kind = SUBMISSION -> ban PV nop gan nhat
    // (bo qua ban EDITED cua BTV). Khong co ban nao -> Optional rong.
    Optional<ArticleVersion> findFirstByArticleIdAndKindOrderByVersionNoDesc(Integer articleId, ArticleVersionKind kind);

    // Ban moi nhat cua bai, moi loai (de lay so version_no tiep theo khi nop)
    Optional<ArticleVersion> findFirstByArticleIdOrderByVersionNoDesc(Integer articleId);

    // B3 (UC-06): ban nop (kind) moi nhat cua moi bai KHAC da nop (trang thai khac DRAFT) de kiem trung noi dung
    @Query("SELECT v FROM ArticleVersion v WHERE v.kind = :kind AND v.articleId <> :articleId"
            + " AND v.articleId IN (SELECT a.id FROM Article a WHERE a.status <> :draftStatus)"
            + " AND v.versionNo = (SELECT MAX(v2.versionNo) FROM ArticleVersion v2"
            + " WHERE v2.articleId = v.articleId AND v2.kind = :kind)")
    List<ArticleVersion> findLatestOfOtherArticles(@Param("articleId") Integer articleId,
                                                   @Param("kind") ArticleVersionKind kind,
                                                   @Param("draftStatus") ArticleStatus draftStatus);
}
