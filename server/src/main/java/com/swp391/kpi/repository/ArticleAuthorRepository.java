package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ArticleAuthor;
import com.swp391.kpi.entity.ArticleAuthorConfirmStatus;
import com.swp391.kpi.entity.ArticleAuthorId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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
}
