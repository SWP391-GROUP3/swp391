package com.swp391.kpi.appeal.repository;
import com.swp391.kpi.appeal.entity.CrossReviewTask;
import org.springframework.data.jpa.repository.JpaRepository;
/**
 * Repository truy van bang cua entity CrossReviewTask (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   13  findByBatchIdOrderById()        tim theo batch id order theo id
 *   14  findByReviewerIdOrderByIdDesc()  tim theo reviewer id order theo id desc
 *   17  findLockedById()                tim locked theo id
 */
public interface CrossReviewTaskRepository extends JpaRepository<CrossReviewTask, Integer> {
    java.util.List<CrossReviewTask> findByBatchIdOrderById(Integer batchId);
    java.util.List<CrossReviewTask> findByReviewerIdOrderByIdDesc(Integer reviewerId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from CrossReviewTask t where t.id=:id")
    java.util.Optional<CrossReviewTask> findLockedById(@org.springframework.data.repository.query.Param("id") Integer id);
}
