package com.swp391.kpi.admin.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/**
 * Ban cua toa soan (bang sections). Chi doc: Admin chon ban khi tao tai khoan.
 * Ban da dung thi tat is_active, khong xoa (quy uoc dau schema.sql).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   29  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String code;            // VARCHAR(20), UNIQUE, vd THOISU

    @Nationalized                   // cot NVARCHAR
    private String name;            // vd "Ban Thoi su"

    private Boolean isActive;       // BIT: ban con hoat dong khong
}
