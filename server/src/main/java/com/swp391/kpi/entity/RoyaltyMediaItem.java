package com.swp391.kpi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Tien 1 anh/video cua 1 phieu cham (bang royalty_media_items, UC-48, BR-RY-04).
 * Chi tao dong cho anh/video DUOC tinh tien: anh nguon ngoai hoac anh tai su dung
 * (trung anh da tung duoc tra nhuan but) thi KHONG tao dong.
 * UNIQUE(grading_id, attachment_id): 1 anh chi tinh 1 lan trong 1 phieu.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   35  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "royalty_media_items")
@Getter
@Setter
@NoArgsConstructor
public class RoyaltyMediaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK trong luong BF-04: nhieu anh/video thuoc 1 phieu cham
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_id")
    private RoyaltyGrading grading;

    // FK sang article_attachments (BF-01): de Integer, thong tin anh lay qua ArticlePublishedService
    private Integer attachmentId;

    private BigDecimal factor;      // DECIMAL(5,2) = media_factors.factor luc cham, chep lai de giai trinh
    private BigDecimal unitPrice;   // DECIMAL(18,0), cung don gia voi phan bai viet
    private BigDecimal amount;      // DECIMAL(18,0) = factor x unitPrice, lam tron xuong 1.000d
}
