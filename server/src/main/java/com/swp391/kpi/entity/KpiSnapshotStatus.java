package com.swp391.kpi.entity;

/**
 * Trang thai cua ban ghi kpi_snapshots (schema.sql).
 * DRAFT = so tam, tinh lai duoc;
 * FINALIZED = da chot, bat bien (BR-KPI-06).
 *
 * MUC LUC (so dong)
 *   12  gia tri                      - DRAFT, FINALIZED
 */
public enum KpiSnapshotStatus {
    DRAFT,
    FINALIZED
}
