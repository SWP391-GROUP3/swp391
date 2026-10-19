package com.swp391.kpi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Su kien lon (bang events, BF-04): bau cu, bao lu...
 * Nhieu bai cung su kien thi bai xep sau thu kLimit trong cua so windowDays bi giam
 * he so bao hoa, tru bai co diem >= exemptScore (GBR-09).
 * Ty le bao hoa cua tung bai da khoa luc nop (articles.saturation_rate, BR-IN-03),
 * nen khi cham nhuan but BF-04 chi can exemptScore de biet bai co duoc mien giam khong.
 *
 * MUC LUC (so dong)
 *   35  field                        - cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nationalized                     // cot NVARCHAR
    private String name;              // vd "Bao so 3 nam 2026"

    private LocalDate startDate;      // DATE
    private LocalDate endDate;        // DATE, >= startDate

    private Integer windowDays;       // cua so tinh bao hoa (ngay), mac dinh 7
    private Integer kLimit;           // so bai dau tien KHONG bi giam, mac dinh 10
    private BigDecimal exemptScore;   // DECIMAL(5,2): diem >= muc nay thi mien giam, mac dinh 90

    private Boolean isActive;

    // FK toi users (luong admin): de Integer, nguoi tao su kien
    private Integer createdBy;
}
