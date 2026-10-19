package com.swp391.kpi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Thong ke tuong tac doc gia theo ngay cua bai viet tu file CSV (bang reader_interactions).
 * Thuoc so huu cua luong kpi (BF-05).
 *
 * MUC LUC (so dong)
 *   31  field                        - cac cot cua bang (khop schema.sql)
 */
@Entity
@Table(name = "reader_interactions")
@Getter
@Setter
@NoArgsConstructor
public class ReaderInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // TODO(article): doi sang @ManyToOne Article khi can
    private Integer articleId;

    private LocalDate recordDate;

    private Integer pageViews;

    private Integer ratingCount;

    private Integer ratingSum;

    // TODO(kpi): doi sang @ManyToOne CsvImportLog khi tao entity csv_import_logs
    private Integer importLogId;

    private LocalDateTime createdAt;
}
