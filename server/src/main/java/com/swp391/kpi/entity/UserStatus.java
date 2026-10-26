package com.swp391.kpi.entity;

/**
 * Trang thai tai khoan (cot users.status, CHECK ck_users_status).
 * Khong xoa user, chi khoa bang LOCKED (comment dau schema.sql).
 *
 * MUC LUC (so dong)
 *   11  gia tri                      - ACTIVE, LOCKED
 */
public enum UserStatus {
    ACTIVE,     // duoc dang nhap
    LOCKED      // bi khoa, khong dang nhap duoc (UC-69)
}
