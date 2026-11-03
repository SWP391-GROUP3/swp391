package com.swp391.kpi.royalty.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalDateTime;

/**
 * 1 dong tien cua 1 nguoi trong 1 ky (bang royalty_lines).
 * Hinh dang dong theo line_type (CHECK ck_royalty_lines_shape):
 *   ARTICLE    co sharePercent, khong co mediaItem/originalLine, amount >= 0
 *   MEDIA      co mediaItem, khong co sharePercent/originalLine, amount >= 0
 *   ADJUSTMENT co originalLine, amount co the am (BR-RY-06)
 * Tong cac dong ARTICLE cua 1 phieu phai bang DUNG grading.amount (GBR-10).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   40  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "royalty_lines")
@Getter
@Setter
@NoArgsConstructor
public class RoyaltyLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK trong luong BF-04: dung @ManyToOne, LAZY
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    private RoyaltyPeriod period;           // ky chua dong tien nay

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_id")
    private RoyaltyGrading grading;         // phieu cham sinh ra dong tien

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_item_id")
    private RoyaltyMediaItem mediaItem;     // chi co voi dong MEDIA, NULL voi loai khac

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_line_id")
    private RoyaltyLine originalLine;       // chi co voi dong ADJUSTMENT: tro dong cu bi dieu chinh

    // NULL cho toi luc Ke toan khoa ky thi gan vao phieu chi cua nguoi nhan (UC-53)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private RoyaltyPayment payment;

    // FK toi users (luong admin): de Integer, nguoi nhan tien
    private Integer userId;

    @Enumerated(EnumType.STRING)            // luu chu "ARTICLE"... xuong DB
    private RoyaltyLineType lineType;

    private BigDecimal sharePercent;        // DECIMAL(5,2), chi co voi dong ARTICLE (vd 70.00)

    private BigDecimal amount;              // DECIMAL(18,0), tien VND

    // Cot co DEFAULT SYSDATETIME() trong DB nhung Hibernate van ghi NULL neu de trong,
    // nen service phai tu gan LocalDateTime.now() truoc khi save.
    private LocalDateTime createdAt;
}
