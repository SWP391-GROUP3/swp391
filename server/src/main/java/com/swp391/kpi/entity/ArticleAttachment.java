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
 * Anh/video cua bai (bang article_attachments).
 * - version_id NULL = dang soan, chua gan vao ban nop nao (comment bang).
 * - author_id phai la 1 tac gia cua bai (BR-IN-05) - kiem o service khi luu.
 * - Khong tinh nhuan but khi: external_source khac NULL (nguon ngoai), hoac
 *   trung (DUPLICATE/SIMILAR) voi anh da tung duoc tra nhuan but (BR-IN-05, BR-RY-04).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   36  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "article_attachments")
@Getter
@Setter
@NoArgsConstructor
public class ArticleAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer articleId;

    private Integer versionId;          // null = anh dang soan

    // TODO(admin): doi sang @ManyToOne User
    private Integer authorId;

    @Enumerated(EnumType.STRING)
    private ArticleMediaType mediaType;

    // Gia tri lay tu bang media_factors (PHOTO, GRAPHIC, VIDEO...), cot khong co CHECK nen de String
    private String mediaKind;

    @Nationalized   // khac null = anh lay tu nguon ngoai, khong tinh tien
    private String externalSource;

    @Nationalized   // duong dan file tren o dia server
    private String url;

    @Nationalized
    private String caption;

    private String fileHash;            // SHA-256, 64 ky tu hex

    private String dhash;               // 64 bit dang hex, chi co voi anh

    @Enumerated(EnumType.STRING)
    private ArticleHashResult hashResult;   // null = chua kiem

    private Byte distanceBits;          // khoang cach Hamming 0-64 (cot TINYINT)

    private Integer matchedAttachmentId;    // anh bi trung, chi co khi DUPLICATE/SIMILAR

    private LocalDateTime createdAt;
}
