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

/**
 * The loai bai (bang genres, luong admin).
 * Luong admin chua co nguoi phu trach nen BF-04 tao ban CHI DOC nay de lay he so chuan
 * (BR-RY-01: he so nhuan but = baseFactor x diem / 100). Man them/sua the loai lam sau.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   32  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "genres")
@Getter
@Setter
@NoArgsConstructor
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String code;            // VARCHAR(30), UNIQUE, vd PHAN_ANH

    @Nationalized                   // cot NVARCHAR
    private String name;            // vd "Bai phan anh"

    @Nationalized
    private String ndGroup;         // nhom the loai theo khung Nghi dinh nhuan but

    private BigDecimal baseFactor;  // DECIMAL(5,2), he so chuan, > 0

    private Boolean isActive;       // BIT: the loai da dung thi tat, khong xoa
}
