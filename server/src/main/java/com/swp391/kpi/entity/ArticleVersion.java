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
 * Ban chup noi dung bai (bang article_versions). Chi INSERT, khong sua, khong xoa.
 * Moi lan PV nop / nop lai -> 1 ban SUBMISSION; BTV bien tap -> 1 ban EDITED.
 *
 * MUC LUC (so dong)
 *   38  id
 *   40  articleId, versionNo   - (article_id, version_no) la UNIQUE
 *   45  kind                   - SUBMISSION / EDITED
 *   48  title, content         - noi dung tai luc chup
 *   54  changeType             - INITIAL / MINOR / MAJOR
 *   57  createdBy, createdAt
 */
@Entity
@Table(name = "article_versions")
@Getter
@Setter
@NoArgsConstructor
public class ArticleVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer articleId;

    private Integer versionNo;          // 1, 2, 3... trong cung 1 bai

    @Enumerated(EnumType.STRING)
    private ArticleVersionKind kind;

    @Nationalized   // cot NVARCHAR
    private String title;

    @Nationalized   // cot NVARCHAR(MAX)
    private String content;

    @Enumerated(EnumType.STRING)
    private ArticleVersionChangeType changeType;

    // Nguoi tao ban nay (PV hoac BTV). TODO(admin): doi sang @ManyToOne User
    private Integer createdBy;

    private LocalDateTime createdAt;
}
