package com.swp391.kpi.royalty.repository;

import java.math.BigDecimal;

/**
 * 1 dong ket qua cua cau SQL RoyaltyPeriodRepository.findPeriodSummaries():
 * tong tien va so bai da cham cua 1 ky (UC-49).
 * Day la "interface projection" cua Spring Data: moi getter lay dung cot co ten bi danh (AS ...)
 * giong ten getter. Khong phai entity, khong co bang.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   17  getPeriodId()                   cot periodId cua cau query
 *   18  getTotalAmount()                cot totalAmount cua cau query
 *   19  getGradedArticleCount()         cot gradedArticleCount cua cau query
 */
public interface RoyaltyPeriodSummaryRow {
    Integer getPeriodId();
    BigDecimal getTotalAmount();
    Integer getGradedArticleCount();
}
