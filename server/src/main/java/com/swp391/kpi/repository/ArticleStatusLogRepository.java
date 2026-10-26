package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ArticleStatus;
import com.swp391.kpi.entity.ArticleStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * MUC LUC (so dong - ten - tac dung)
 *   18  findByArticleIdOrderByCreatedAtAscIdAsc()  Lich su trang thai cua 1 bai, cu nhat len dau (UC-02)
 *   21  findFirstByArticleIdAndToStatusOrderByCreatedAtDescIdDesc()  Lan gan nhat bai duoc chuyen sang 1 trang thai (vd lay ghi chu luc bi tra ve)
 */
public interface ArticleStatusLogRepository extends JpaRepository<ArticleStatusLog, Integer> {

    // Lich su trang thai cua 1 bai, cu nhat len dau (UC-02)
    List<ArticleStatusLog> findByArticleIdOrderByCreatedAtAscIdAsc(Integer articleId);

    // Lan gan nhat bai duoc chuyen sang 1 trang thai (vd lay ghi chu luc bi tra ve)
    Optional<ArticleStatusLog> findFirstByArticleIdAndToStatusOrderByCreatedAtDescIdDesc(Integer articleId,
                                                                                         ArticleStatus toStatus);
}
