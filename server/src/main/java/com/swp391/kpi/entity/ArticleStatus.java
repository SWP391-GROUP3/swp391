package com.swp391.kpi.entity;

/**
 * Trang thai bai viet (cot articles.status).
 * Ten hang trung dung gia tri trong CHECK ck_articles_status cua schema.sql.
 *
 * MUC LUC (so dong)
 *   11  gia tri                      - DRAFT, PENDING_CONFIRMATION, SUBMITTED, ASSIGNED, REVISION_REQUESTED, R…
 */
public enum ArticleStatus {
    DRAFT,                // ban nhap, tac gia chinh con sua duoc (BR-IN-01)
    PENDING_CONFIRMATION, // da nop, cho dong tac gia xac nhan trong 3 ngay (BR-IN-06)
    SUBMITTED,            // da nop, cho Truong ban phan cong BTV
    ASSIGNED,             // da phan cong BTV (BF-02)
    REVISION_REQUESTED,   // BTV tra ve cho sua
    REJECTED,             // bi tu choi, ket thuc (BR-IN-08)
    PENDING_APPROVAL,     // BTV cham xong, cho Truong ban duyet diem
    SCORE_PUBLISHED,      // da cong bo diem
    PUBLISHED             // da dang
}
