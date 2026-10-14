package com.swp391.kpi.repository;

import com.swp391.kpi.entity.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppealRepository extends JpaRepository<Appeal, Integer> {

    boolean existsByArticleId(Integer articleId);

    List<Appeal> findAllByAppellantIdOrderBySubmittedAtDesc(Integer appellantId);
}
