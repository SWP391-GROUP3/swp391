package com.swp391.kpi.repository;
import com.swp391.kpi.entity.CrossReviewTask;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CrossReviewTaskRepository extends JpaRepository<CrossReviewTask, Integer> {
    java.util.List<CrossReviewTask> findByBatchIdOrderById(Integer batchId);
    java.util.List<CrossReviewTask> findByReviewerIdOrderByIdDesc(Integer reviewerId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from CrossReviewTask t where t.id=:id")
    java.util.Optional<CrossReviewTask> findLockedById(@org.springframework.data.repository.query.Param("id") Integer id);
}
