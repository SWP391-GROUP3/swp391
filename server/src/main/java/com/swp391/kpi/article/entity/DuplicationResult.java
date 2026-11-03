package com.swp391.kpi.article.entity;

/**
 * Ket qua 1 lan kiem trung noi dung (cot duplication_checks.result, BR-IN-04).
 * Ten hang trung dung gia tri trong CHECK ck_duplication_checks_result cua schema.sql.
 *
 * MUC LUC (so dong - ten - tac dung)
 *  ---- Gia tri (trung CHECK trong schema.sql) ----
 *   14  NORMAL                          trung duoi DUP_WARN_PERCENT (20%)
 *   15  NEED_EXPLAIN                    20-50%, hoac tren 50% nhung tinh chat khac ORIGINAL: PV phai giai trinh
 *   16  BLOCKED                         tren 50% va tinh chat ORIGINAL: chan nop bai
 */
public enum DuplicationResult {
    NORMAL,        // trung duoi DUP_WARN_PERCENT (20%)
    NEED_EXPLAIN,  // 20-50%, hoac tren 50% nhung tinh chat khac ORIGINAL: PV phai giai trinh
    BLOCKED        // tren 50% va tinh chat ORIGINAL: chan nop bai
}
