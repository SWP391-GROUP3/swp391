package com.swp391.kpi.article.repository;

import com.swp391.kpi.article.entity.ArticleAuthor;
import com.swp391.kpi.article.entity.ArticleAuthorConfirmStatus;
import com.swp391.kpi.article.entity.ArticleAuthorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository truy van bang cua entity ArticleAuthor (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  findByArticleIdOrderBySharePercentDesc()  Moi tac gia cua 1 bai, ty le cao xep truoc
 *   27  existsByArticleIdAndUserId()    Nguoi dung co phai la 1 tac gia cua bai khong (BR-IN-01: dong tac gia duoc xem)
 *   30  findByUserIdAndArticleIdIn()    Dong cua nguoi dung trong nhieu bai cung luc (lay vai tro, ty le cho trang danh sach)
 *   33  findByUserIdAndConfirmStatusOrderByConfirmDeadlineAsc()  Loi moi dong tac gia cua nguoi dung theo trang thai (vd PENDING), sap het han xep truoc
 *   37  findByConfirmStatusAndConfirmDeadlineBefore()  Dong dang cho xac nhan ma da qua han (job het han 3 ngay, UC-12 A2)
 */
public interface ArticleAuthorRepository extends JpaRepository<ArticleAuthor, ArticleAuthorId> {

    // Moi tac gia cua 1 bai, ty le cao xep truoc
    List<ArticleAuthor> findByArticleIdOrderBySharePercentDesc(Integer articleId);

    // Nguoi dung co phai la 1 tac gia cua bai khong (BR-IN-01: dong tac gia duoc xem)
    boolean existsByArticleIdAndUserId(Integer articleId, Integer userId);

    // Dong cua nguoi dung trong nhieu bai cung luc (lay vai tro, ty le cho trang danh sach)
    List<ArticleAuthor> findByUserIdAndArticleIdIn(Integer userId, List<Integer> articleIds);

    // Loi moi dong tac gia cua nguoi dung theo trang thai (vd PENDING), sap het han xep truoc
    List<ArticleAuthor> findByUserIdAndConfirmStatusOrderByConfirmDeadlineAsc(Integer userId,
                                                                              ArticleAuthorConfirmStatus confirmStatus);

    // Dong dang cho xac nhan ma da qua han (job het han 3 ngay, UC-12 A2)
    List<ArticleAuthor> findByConfirmStatusAndConfirmDeadlineBefore(ArticleAuthorConfirmStatus confirmStatus,
                                                                    LocalDateTime time);
}
