package com.swp391.kpi.entity;

/**
 * Trang thai tai khoan (cot users.status, CHECK ck_users_status).
 * Khong xoa user, chi khoa bang LOCKED (comment dau schema.sql).
 */
public enum UserStatus {
    ACTIVE,     // duoc dang nhap
    LOCKED      // bi khoa, khong dang nhap duoc (UC-69)
}
