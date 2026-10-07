package com.swp391.kpi.royalty.entity;

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

import java.time.LocalDateTime;

/**
 * Anh xa bang royalty_periods: moi thang 1 ky nhuan but.
 */
@Entity
@Table(name = "royalty_periods")
@Getter
@Setter
@NoArgsConstructor
public class RoyaltyPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Kieu Java phai khop kieu cot: SMALLINT -> Short, TINYINT -> Byte.
    // Sai la ddl-auto=validate bao loi ngay luc khoi dong.
    private Short periodYear;

    private Byte periodMonth;

    @Enumerated(EnumType.STRING)
    private RoyaltyPeriodStatus status;

    // Tam luu id nguoi dung. Khi luong Admin co entity User
    // thi doi sang @ManyToOne(fetch = FetchType.LAZY).
    private Integer lockedBy;

    private LocalDateTime lockedAt;

    // Ke toan tra ky CLOSED ve OPEN: ghi nguoi tra va ly do (lan gan nhat)
    private Integer returnedBy;

    private String returnNote;

    private LocalDateTime paidAt;
}
