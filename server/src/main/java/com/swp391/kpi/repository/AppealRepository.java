package com.swp391.kpi.repository;

import com.swp391.kpi.entity.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppealRepository extends JpaRepository<Appeal, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appeal a where a.id = :id")
    Optional<Appeal> findLockedById(@Param("id") Integer id);

    boolean existsByArticleId(Integer articleId);

    List<Appeal> findAllByAppellantIdOrderBySubmittedAtDesc(Integer appellantId);
}
