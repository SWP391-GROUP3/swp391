package com.swp391.kpi.repository;

import com.swp391.kpi.entity.CrossReviewBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/**
 * Repository truy van bang cua entity CrossReviewBatch (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  findLockedById()                tim locked theo id
 *   25  findNameLocked()                Khoa ca khoang ten chua ton tai de hai request khong tao trung dot
 *   27  findAllByOrderByCreatedAtDescIdDesc()  tim tat ca theo order theo created at desc id desc
 */
public interface CrossReviewBatchRepository extends JpaRepository<CrossReviewBatch, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from CrossReviewBatch b where b.id=:id")
    Optional<CrossReviewBatch> findLockedById(@Param("id") Integer id);

    // Khoa ca khoang ten chua ton tai de hai request khong tao trung dot.
    @Query(value = "select * from cross_review_batches with (updlock,holdlock) where batch_name=:name", nativeQuery = true)
    List<CrossReviewBatch> findNameLocked(@Param("name") String name);

    List<CrossReviewBatch> findAllByOrderByCreatedAtDescIdDesc();
}
