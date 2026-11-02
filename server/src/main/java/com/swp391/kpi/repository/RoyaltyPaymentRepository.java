package com.swp391.kpi.repository;

import com.swp391.kpi.entity.RoyaltyPayment;
import com.swp391.kpi.entity.RoyaltyPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Doc/ghi bang royalty_payments (phieu chi tung nguoi trong ky, UC-53, UC-54).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   20  findByPeriodId()                Cac phieu chi cua 1 ky: khoa ky sau lay no chuyen tiep (debt_out)
 *   23  findByPeriodIdOrderByUserIdAsc() UC-54: danh sach phieu chi cua 1 ky cho Ke toan, xep theo nguoi nhan
 *   26  countByPeriodIdAndStatus()      UC-54: con bao nhieu phieu UNPAID -> het thi ky sang PAID
 *   29  findByUserId()                  UC-57: cac phieu chi cua 1 nguoi qua moi ky
 */
public interface RoyaltyPaymentRepository extends JpaRepository<RoyaltyPayment, Integer> {

    // Cac phieu chi cua 1 ky: khoa ky sau lay no chuyen tiep (debt_out)
    List<RoyaltyPayment> findByPeriodId(Integer periodId);

    // UC-54: danh sach phieu chi cua 1 ky cho Ke toan, xep theo nguoi nhan
    List<RoyaltyPayment> findByPeriodIdOrderByUserIdAsc(Integer periodId);

    // UC-54: con bao nhieu phieu UNPAID -> het thi ky sang PAID
    long countByPeriodIdAndStatus(Integer periodId, RoyaltyPaymentStatus status);

    // UC-57: cac phieu chi cua 1 nguoi qua moi ky
    List<RoyaltyPayment> findByUserId(Integer userId);
}
