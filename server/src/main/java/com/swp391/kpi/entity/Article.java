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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bai viet (bang articles). title/content la ban dang soan, sua thoai mai;
 * moi lan nop thi chup lai vao article_versions.
 * Rut bai: SUBMITTED -> DRAFT, ghi withdrawn_at (BR-IN-08).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   35  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Tac gia chinh: nguoi tao, nop, rut, nop lai.
    // TODO(admin): doi sang @ManyToOne User khi luong admin co entity User
    private Integer authorId;

    // Ban cua tac gia chinh tai luc tao bai. TODO(admin): doi sang @ManyToOne Section
    private Integer sectionId;

    // The loai PV khai / the loai ap dung (BTV doi duoc). TODO(admin): doi sang @ManyToOne Genre
    private Integer declaredCategoryId;

    private Integer categoryId;

    // Su kien (bang events cua luong royalty), null neu khong thuoc su kien nao
    private Integer eventId;

    // Truong ban duyet dang (BF-02). TODO(admin): doi sang @ManyToOne User
    private Integer approvedBy;

    @Nationalized   // cot NVARCHAR
    private String title;

    @Nationalized   // cot NVARCHAR(MAX)
    private String content;

    @Enumerated(EnumType.STRING)
    private ArticleNature declaredNature;

    @Enumerated(EnumType.STRING)
    private ArticleStatus status;

    // Thu hang va ty le bao hoa khi bai thuoc su kien, null neu khong thuoc su kien
    private Integer saturationRank;

    private BigDecimal saturationRate;

    private LocalDateTime submittedAt;   // lan nop gan nhat

    private LocalDateTime withdrawnAt;   // lan rut gan nhat

    private LocalDateTime publishedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
