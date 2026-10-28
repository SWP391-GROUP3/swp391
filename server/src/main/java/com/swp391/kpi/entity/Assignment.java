package com.swp391.kpi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Ghi nhận Trưởng ban giao một bài viết cho một BTV chấm chất lượng.
 * Bảng: assignments
 *
 * MUC LUC (so dong - ten - tac dung)
 *   26  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK -> articles(id), mỗi bài chỉ có 1 phân công duy nhất (UNIQUE)
    @Column(name = "article_id", nullable = false, unique = true)
    private Integer articleId;

    // FK -> users(id): Biên tập viên được phân công
    @Column(name = "editor_id", nullable = false)
    private Integer editorId;

    // FK -> users(id): Trưởng ban thực hiện giao bài
    @Column(name = "assigned_by", nullable = false)
    private Integer assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt = LocalDateTime.now();

    // 1 nếu chọn từ danh sách BTV do hệ thống gợi ý, 0 nếu Trưởng ban tự chọn
    @Column(name = "was_suggested", nullable = false)
    private Boolean wasSuggested = false;
}
