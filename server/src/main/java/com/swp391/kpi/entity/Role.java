package com.swp391.kpi.entity;

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
import org.hibernate.annotations.Nationalized;

/**
 * Vai tro co dinh (bang roles). Chi doc, khong co man hinh them/sua vai tro.
 *
 * MUC LUC (so dong)
 *   30  field                        - cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private RoleCode code;      // VARCHAR(20): REPORTER, EDITOR, ...

    @Nationalized               // cot NVARCHAR
    private String name;        // ten tieng Viet: "Phong vien", ...
}
