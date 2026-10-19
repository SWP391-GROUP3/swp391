package com.swp391.kpi.entity;

/**
 * Trạng thái của phiên bản bộ tiêu chí chấm.
 * Khớp với CHECK (status IN ('DRAFT','ACTIVE','RETIRED')) trong schema.sql.
 *
 * MUC LUC (so dong)
 *   11  gia tri                      - DRAFT, ACTIVE, RETIRED
 */
public enum CriteriaSetStatus {
    DRAFT,
    ACTIVE,
    RETIRED
}
