package com.swp391.kpi.kpi.entity;

/**
 * Trang thai cua ban ghi kpi_snapshots (schema.sql).
 * DRAFT = so tam, tinh lai duoc;
 * FINALIZED = da chot, bat bien (BR-KPI-06).
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   14  DRAFT
 *   15  FINALIZED
 */
public enum KpiSnapshotStatus {
    DRAFT,
    FINALIZED
}
