package com.swp391.kpi.repository;

import com.swp391.kpi.entity.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MUC LUC (so dong)
 *   15  existsByArticleId()
 *   17  findAllByAppellantIdOrderBySubmittedAtDesc()
 */
public interface AppealRepository extends JpaRepository<Appeal, Integer> {

    boolean existsByArticleId(Integer articleId);

    List<Appeal> findAllByAppellantIdOrderBySubmittedAtDesc(Integer appellantId);
}
