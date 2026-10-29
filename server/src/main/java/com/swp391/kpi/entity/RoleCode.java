package com.swp391.kpi.entity;

/**
 * Ma vai tro (cot roles.code). Ten hang trung dung gia tri trong data.sql.
 * Moi user dung 1 vai tro (users.role_id).
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   17  REPORTER                        Phong vien
 *   18  EDITOR                          Bien tap vien
 *   19  HEAD_OF_DEPARTMENT              Truong ban
 *   20  EDITORIAL_SECRETARY             Thu ky toa soan
 *   21  ACCOUNTANT                      Ke toan
 *   22  ADMIN                           Quan tri he thong
 */
public enum RoleCode {
    REPORTER,               // Phong vien
    EDITOR,                 // Bien tap vien
    HEAD_OF_DEPARTMENT,     // Truong ban
    EDITORIAL_SECRETARY,    // Thu ky toa soan
    ACCOUNTANT,             // Ke toan
    ADMIN                   // Quan tri he thong
}
