package com.swp391.kpi.article.repository;

import com.swp391.kpi.article.entity.DuplicationCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Bang duplication_checks (BF-01): ket qua kiem trung noi dung moi lan nop (UC-06), ghi luc nop bai
 * (DuplicationCheckService.saveResult()).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  findFirstByArticleIdOrderByIdDesc()  Lan kiem gan nhat cua bai (ung voi lan nop gan nhat)
 */
public interface DuplicationCheckRepository extends JpaRepository<DuplicationCheck, Integer> {

    // Lan kiem gan nhat cua bai (ung voi lan nop gan nhat)
    Optional<DuplicationCheck> findFirstByArticleIdOrderByIdDesc(Integer articleId);
}
