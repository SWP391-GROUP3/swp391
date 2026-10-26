package com.swp391.kpi.entity;

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
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Phieu cham nhuan but phan bai viet (bang royalty_gradings, UC-47).
 * Moi lan tinh lai la 1 PHIEN BAN MOI (version_no tang, bat buoc recalc_reason),
 * KHONG sua phien ban cu. Luu du tung buoc cong thuc de giai trinh (BR-RY-01..03).
 *
 * MUC LUC (so dong)
 *   38  field                        - cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "royalty_gradings")
@Getter
@Setter
@NoArgsConstructor
public class RoyaltyGrading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // FK sang bang cua luong khac (BF-01, BF-02, admin): de Integer, can du lieu
    // thi goi Service cua luong do (AGENTS.md muc 2), khong di qua entity cua ho.
    private Integer scoringAttemptId;   // lan cham co diem dang dung
    private Integer articleId;
    private Integer categoryId;         // the loai ap dung luc cham (genres)
    private Integer gradedBy;           // Thu ky toa soan (users)

    private Integer versionNo;          // 1, 2, 3... UNIQUE(article_id, version_no)

    // FK trong luong BF-04: dung @ManyToOne, LAZY = chi query khi goi getPeriod()
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id")
    private RoyaltyPeriod period;       // ky dang OPEN luc hoan tat cham (BR-RY-05)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rate_id")
    private RoyaltyRate rate;           // don gia hieu luc tai ngay dang bai

    // Cac buoc cong thuc, DECIMAL(5,2)
    private BigDecimal scoreUsed;       // diem hieu luc, thang 0-100
    private BigDecimal tierRate;        // nhom da bo chia bac: luu bang diem (SRS ghi chu BR-RY-01)
    private BigDecimal baseFactor;      // he so chuan cua the loai
    private BigDecimal suggestedFactor; // = baseFactor x diem / 100
    private BigDecimal appliedFactor;   // luon = suggestedFactor (Thu ky khong nang nua)

    @Nationalized                       // cot NVARCHAR
    private String raiseReason;         // luon NULL vi khong nang he so

    private BigDecimal saturationRate;  // % bao hoa su kien, 100 neu khong giam

    @Enumerated(EnumType.STRING)        // luu chu "ORIGINAL"... xuong DB
    private RoyaltyNature nature;       // tinh chat bai Thu ky xac nhan

    private BigDecimal natureRate;      // % theo tinh chat
    private BigDecimal finalFactor;     // he so cuoi, toi thieu 1

    private BigDecimal unitPrice;       // DECIMAL(18,0), chep tu rate de giai trinh
    private BigDecimal amount;          // DECIMAL(18,0), tien phan bai viet

    @Nationalized
    private String recalcReason;        // bat buoc tu phien ban 2 tro di

    private LocalDateTime gradedAt;
}
