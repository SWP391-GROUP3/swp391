package com.swp391.kpi.article.repository;

import com.swp391.kpi.article.entity.ArticleStatus;
import com.swp391.kpi.article.entity.ArticleStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy van bang cua entity ArticleStatusLog (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  findByArticleIdOrderByCreatedAtAscIdAsc()  Lich su trang thai cua 1 bai, cu nhat len dau (UC-02)
 *   24  findFirstByArticleIdAndToStatusOrderByCreatedAtDescIdDesc()  Lan gan nhat bai duoc chuyen sang 1 trang thai (vd lay ghi chu luc bi tra ve)
 *   28  findFirstByArticleIdAndActionOrderByCreatedAtDescIdDesc()  Lan gan nhat bai co 1 hanh dong (vd SUBMIT): moc khoa hang bao hoa = log SUBMIT moi nhat (quyet dinh 03/11)
 */
public interface ArticleStatusLogRepository extends JpaRepository<ArticleStatusLog, Integer> {

    // Lich su trang thai cua 1 bai, cu nhat len dau (UC-02)
    List<ArticleStatusLog> findByArticleIdOrderByCreatedAtAscIdAsc(Integer articleId);

    // Lan gan nhat bai duoc chuyen sang 1 trang thai (vd lay ghi chu luc bi tra ve)
    Optional<ArticleStatusLog> findFirstByArticleIdAndToStatusOrderByCreatedAtDescIdDesc(Integer articleId,
                                                                                         ArticleStatus toStatus);

    // Lan gan nhat bai co 1 hanh dong (vd SUBMIT): moc khoa hang bao hoa = log SUBMIT moi nhat (quyet dinh 03/11)
    Optional<ArticleStatusLog> findFirstByArticleIdAndActionOrderByCreatedAtDescIdDesc(Integer articleId,
                                                                                       String action);
}
