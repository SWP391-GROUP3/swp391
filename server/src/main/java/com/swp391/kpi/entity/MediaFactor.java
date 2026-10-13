package com.swp391.kpi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * He so co dinh cua tung loai anh/video (bang media_factors, BR-RY-04, UC-56).
 * Cot max_factor trong DB: nhom da bo (khong dung), cho PR [DB] xoa cot va CHECK di kem.
 */
@Entity
@Table(name = "media_factors")
@Getter
@Setter
@NoArgsConstructor
public class MediaFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String mediaKind;   // VARCHAR(20), UNIQUE: PHOTO / GRAPHIC / VIDEO

    private BigDecimal factor;  // DECIMAL(5,2): he so cua loai nay
}
