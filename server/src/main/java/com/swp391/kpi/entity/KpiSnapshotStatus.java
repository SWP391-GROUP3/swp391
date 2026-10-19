package com.swp391.kpi.entity;

/**
 * Trang thai cua ban ghi kpi_snapshots (schema.sql).
 * DRAFT = so tam, tinh lai duoc;
 * FINALIZED = da chot, bat bien (BR-KPI-06).
 */
public enum KpiSnapshotStatus {
    DRAFT,
    FINALIZED
}
