package com.swp391.kpi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Lịch sử Biên tập viên đổi thể loại bài viết trong lúc xử lý/chấm.
 * Bảng: category_change_logs
 *
 * MUC LUC (so dong - ten - tac dung)
 *   26  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "category_change_logs")
@Getter
@Setter
@NoArgsConstructor
public class CategoryChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    // FK -> genres(id)
    @Column(name = "from_category_id", nullable = false)
    private Integer fromCategoryId;

    // FK -> genres(id)
    @Column(name = "to_category_id", nullable = false)
    private Integer toCategoryId;

    // FK -> users(id)
    @Column(name = "changed_by", nullable = false)
    private Integer changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();
}
