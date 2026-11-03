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

import java.time.LocalDateTime;

/**
 * Tham so he thong dang key-value (bang system_parameters), vd NATURE_RATE_TRANSLATION = 70.
 * Code doc theo param_key, khong viet cung so trong Java.
 * Admin sua 5 tham so nhap bai cua BF-01 o UC-13 (SystemParameterService), ghi lai ai sua va luc nao.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   32  field                           cac cot cua bang (khop schema.sql)
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

    @Nationalized               // cot NVARCHAR(20)
    private String unit;        // don vi hien thi: người, ngày, %, bit... NULL = khong co

    private String flow;        // luong dung tham so: BF-01 ... BF-05, NULL = dung chung

    // FK toi users: de Integer. Admin sua lan cuoi; NULL = du lieu mau / migration, chua ai sua
    private Integer updatedBy;

    private LocalDateTime updatedAt;  // DATETIME2, DB mac dinh SYSDATETIME() luc them
}
