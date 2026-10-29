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

import java.time.LocalDateTime;

/**
 * Nhat ky moi lan nhap file CSV so lieu doc gia (bang csv_import_logs).
 * Thuoc so huu cua luong kpi (BF-05).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   31  field                           cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "csv_import_logs")
@Getter
@Setter
@NoArgsConstructor
public class CsvImportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nationalized
    private String fileName;

    private Integer totalRecords;

    private Integer successRecords;

    private Integer importedBy;

    private LocalDateTime importedAt;
}
