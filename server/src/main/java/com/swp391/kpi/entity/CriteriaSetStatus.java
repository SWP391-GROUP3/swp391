package com.swp391.kpi.entity;

/**
 * Trạng thái của phiên bản bộ tiêu chí chấm.
 * Khớp với CHECK (status IN ('DRAFT','ACTIVE','RETIRED')) trong schema.sql.
 */
public enum CriteriaSetStatus {
    DRAFT,
    ACTIVE,
    RETIRED
}
