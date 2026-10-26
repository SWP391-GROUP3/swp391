package com.swp391.kpi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/**
 * Lich su xu ly bai (bang article_status_logs). Chi INSERT, khong sua, khong xoa.
 * Moi lan doi articles.status phai ghi 1 dong o day (AGENTS.md muc 2).
 *
 * MUC LUC (so dong)
 *   33  field                        - cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "article_status_logs")
@Getter
@Setter
@NoArgsConstructor
public class ArticleStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer articleId;

    // Nguoi thao tac. TODO(admin): doi sang @ManyToOne User
    private Integer actorId;

    // Hanh dong: CREATE, SUBMIT, WITHDRAW, ASSIGN, RETURN, REJECT, PUBLISH...
    // Cot khong co CHECK va luong khac cung ghi vao nen de String, khong dung enum.
    private String action;

    @Enumerated(EnumType.STRING)
    private ArticleStatus fromStatus;   // null o lan tao bai

    @Enumerated(EnumType.STRING)
    private ArticleStatus toStatus;

    @Nationalized   // cot NVARCHAR, bat buoc khi tra bai hoac tu choi
    private String note;

    private LocalDateTime createdAt;
}
