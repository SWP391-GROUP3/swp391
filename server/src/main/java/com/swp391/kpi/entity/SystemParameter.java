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

/**
 * Tham so he thong dang key-value (bang system_parameters), vd NATURE_RATE_TRANSLATION = 70.
 * Code doc theo param_key, khong viet cung so trong Java.
 * Hien chi anh xa cac cot can de DOC; cot unit, updated_by, updated_at them khi lam man Admin sua tham so.
 */
@Entity
@Table(name = "system_parameters")
@Getter
@Setter
@NoArgsConstructor
public class SystemParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String paramKey;    // VARCHAR(50), UNIQUE

    @Nationalized               // cot NVARCHAR
    private String paramValue;  // luu dang chu, code tu doi sang so

    private String flow;        // luong dung tham so: BF-01 ... BF-05, NULL = dung chung
}
