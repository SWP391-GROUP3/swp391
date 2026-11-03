package com.swp391.kpi.article.repository;

import com.swp391.kpi.article.entity.DuplicationMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Bang duplication_matches (BF-01): cac bai bi trung cua 1 lan kiem trung noi dung (UC-06, UC-08).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   17  findByCheckIdOrderByOverlapPercentDesc()  Cac bai bi trung cua 1 lan kiem, trung nhieu xep truoc
 */
public interface DuplicationMatchRepository extends JpaRepository<DuplicationMatch, Integer> {

    // Cac bai bi trung cua 1 lan kiem, trung nhieu xep truoc
    List<DuplicationMatch> findByCheckIdOrderByOverlapPercentDesc(Integer checkId);
}
