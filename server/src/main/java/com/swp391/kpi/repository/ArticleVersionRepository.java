package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ArticleVersion;
import com.swp391.kpi.entity.ArticleVersionKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * MUC LUC (so dong - ten - tac dung)
 *   18  findFirstByArticleIdAndKindOrderByVersionNoDesc()  Ban moi nhat cua bai theo loai
 *   21  findFirstByArticleIdOrderByVersionNoDesc()  Ban moi nhat cua bai, moi loai (de lay so version_no tiep theo khi nop)
 */
public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, Integer> {

    // Ban moi nhat cua bai theo loai. Vd kind = SUBMISSION -> ban PV nop gan nhat
    // (bo qua ban EDITED cua BTV). Khong co ban nao -> Optional rong.
    Optional<ArticleVersion> findFirstByArticleIdAndKindOrderByVersionNoDesc(Integer articleId, ArticleVersionKind kind);

    // Ban moi nhat cua bai, moi loai (de lay so version_no tiep theo khi nop)
    Optional<ArticleVersion> findFirstByArticleIdOrderByVersionNoDesc(Integer articleId);
}
