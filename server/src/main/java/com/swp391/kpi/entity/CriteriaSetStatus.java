package com.swp391.kpi.entity;

/**
 * Trạng thái của phiên bản bộ tiêu chí chấm.
 * Khớp với CHECK (status IN ('DRAFT','ACTIVE','RETIRED')) trong schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   14  DRAFT
 *   15  ACTIVE
 *   16  RETIRED
 */
public enum CriteriaSetStatus {
    DRAFT,
    ACTIVE,
    RETIRED
}
