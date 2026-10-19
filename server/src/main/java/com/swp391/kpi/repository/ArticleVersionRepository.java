package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ArticleVersion;
import com.swp391.kpi.entity.ArticleVersionKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * MUC LUC (so dong)
 *   17  findFirstByArticleIdAndKindOrderByVersionNoDesc - ban moi nhat cua 1 loai (vd ban nop moi nhat)
 */
public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, Integer> {

    // Ban moi nhat cua bai theo loai. Vd kind = SUBMISSION -> ban PV nop gan nhat
    // (bo qua ban EDITED cua BTV). Khong co ban nao -> Optional rong.
    Optional<ArticleVersion> findFirstByArticleIdAndKindOrderByVersionNoDesc(Integer articleId, ArticleVersionKind kind);
}
