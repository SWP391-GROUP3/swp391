package com.swp391.kpi.royalty.entity;

/**
 * Trang thai ky nhuan but. Thu tu chuyen: OPEN -> CLOSED -> LOCKED -> PAID.
 * Ten phai trung dung voi gia tri trong CHECK cua bang royalty_periods.
 */
public enum RoyaltyPeriodStatus {
    OPEN,
    CLOSED,
    LOCKED,
    PAID
}
