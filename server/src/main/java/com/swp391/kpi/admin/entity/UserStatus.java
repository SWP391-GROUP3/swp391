package com.swp391.kpi.admin.entity;

/**
 * Trang thai tai khoan (cot users.status, CHECK ck_users_status).
 * Khong xoa user, chi khoa bang LOCKED (comment dau schema.sql).
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   13  ACTIVE                          duoc dang nhap
 *   14  LOCKED                          bi khoa, khong dang nhap duoc (UC-69)
 */
public enum UserStatus {
    ACTIVE,     // duoc dang nhap
    LOCKED      // bi khoa, khong dang nhap duoc (UC-69)
}
