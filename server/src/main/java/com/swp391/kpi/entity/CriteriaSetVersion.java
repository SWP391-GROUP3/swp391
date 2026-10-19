package com.swp391.kpi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Phiên bản bộ tiêu chí chấm của một thể loại bài viết.
 * Bảng: criteria_set_versions
 */
@Entity
@Table(name = "criteria_set_versions")
@Getter
@Setter
@NoArgsConstructor
public class CriteriaSetVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK -> genres(id) (chưa có entity Genre nên tạm để Integer theo AGENTS.md)
    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private CriteriaSetStatus status = CriteriaSetStatus.DRAFT;

    // FK -> users(id)
    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // FK -> users(id)
    @Column(name = "activated_by")
    private Integer activatedBy;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;
}
