package com.swp391.kpi.repository;

import com.swp391.kpi.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Integer> {

    // Tìm lượt phân công theo mã bài viết (quan hệ 1-1)
    Optional<Assignment> findByArticleId(Integer articleId);

    // Lấy danh sách bài viết được phân công cho một BTV (UC-21)
    List<Assignment> findByEditorIdOrderByAssignedAtDesc(Integer editorId);

    // Đếm số bài viết mà một BTV đang phụ trách (chưa hoàn thành) để gợi ý BTV ít việc nhất (UC-19, BR-QA-04)
    @Query("SELECT COUNT(a) FROM Assignment a WHERE a.editorId = :editorId")
    long countAssignedArticlesByEditorId(@Param("editorId") Integer editorId);
}
