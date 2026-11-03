package com.swp391.kpi.appeal.repository;

import com.swp391.kpi.appeal.entity.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository truy van bang cua entity Appeal (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  findByArticleId()               tim theo article id
 *   28  findLockedById()                tim locked theo id
 *   30  existsByArticleId()             kiem tra ton tai theo article id
 *   32  findAllByAppellantIdOrderBySubmittedAtDesc()  tim tat ca theo appellant id order theo submitted at desc
 */
public interface AppealRepository extends JpaRepository<Appeal, Integer> {

    Optional<Appeal> findByArticleId(Integer articleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Appeal a where a.id = :id")
    Optional<Appeal> findLockedById(@Param("id") Integer id);

    boolean existsByArticleId(Integer articleId);

    List<Appeal> findAllByAppellantIdOrderBySubmittedAtDesc(Integer appellantId);
}
