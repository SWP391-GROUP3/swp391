package com.swp391.kpi.entity;

/**
 * Ma vai tro (cot roles.code). Ten hang trung dung gia tri trong data.sql.
 * Moi user dung 1 vai tro (users.role_id).
 */
public enum RoleCode {
    REPORTER,               // Phong vien
    EDITOR,                 // Bien tap vien
    HEAD_OF_DEPARTMENT,     // Truong ban
    EDITORIAL_SECRETARY,    // Thu ky toa soan
    ACCOUNTANT,             // Ke toan
    ADMIN                   // Quan tri he thong
}
