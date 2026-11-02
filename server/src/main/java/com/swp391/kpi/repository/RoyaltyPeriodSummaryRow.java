package com.swp391.kpi.repository;

import java.math.BigDecimal;

/**
 * 1 dong ket qua cua cau SQL RoyaltyPeriodRepository.findPeriodSummaries():
 * tong tien va so bai da cham cua 1 ky (UC-49).
 * Day la "interface projection" cua Spring Data: moi getter lay dung cot co ten bi danh (AS ...)
 * giong ten getter. Khong phai entity, khong co bang.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  getPeriodId()                   cot periodId: id ky
 *   19  getTotalAmount()                cot totalAmount: tong moi dong tien cua ky (0 neu chua co dong nao)
 *   20  getGradedArticleCount()         cot gradedArticleCount: so bai da cham (cham lai van tinh 1 bai)
 */
public interface RoyaltyPeriodSummaryRow {
    Integer getPeriodId();
    BigDecimal getTotalAmount();
    Integer getGradedArticleCount();
}
