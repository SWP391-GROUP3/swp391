package com.swp391.kpi.royalty.repository;

import com.swp391.kpi.royalty.entity.RoyaltyPayment;
import com.swp391.kpi.royalty.entity.RoyaltyPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Doc/ghi bang royalty_payments (phieu chi tung nguoi trong ky, UC-53, UC-54).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  findByPeriodId()                Cac phieu chi cua 1 ky: khoa ky sau lay no chuyen tiep (debt_out)
 *   25  findByPeriodIdOrderByUserIdAsc() UC-54: danh sach phieu chi cua 1 ky cho Ke toan, xep theo nguoi nhan
 *   28  countByPeriodIdAndStatus()      UC-54: con bao nhieu phieu UNPAID -> het thi ky sang PAID
 *   32  findMyPayments()                UC-57: cac phieu chi cua 1 nguoi qua moi ky, nap san ky (JOIN FETCH)
 */
public interface RoyaltyPaymentRepository extends JpaRepository<RoyaltyPayment, Integer> {

    // Cac phieu chi cua 1 ky: khoa ky sau lay no chuyen tiep (debt_out)
    List<RoyaltyPayment> findByPeriodId(Integer periodId);

    // UC-54: danh sach phieu chi cua 1 ky cho Ke toan, xep theo nguoi nhan
    List<RoyaltyPayment> findByPeriodIdOrderByUserIdAsc(Integer periodId);

    // UC-54: con bao nhieu phieu UNPAID -> het thi ky sang PAID
    long countByPeriodIdAndStatus(Integer periodId, RoyaltyPaymentStatus status);

    // UC-57: cac phieu chi cua 1 nguoi qua moi ky. JOIN FETCH nap luon ky (tranh N+1 khi goi getPeriod())
    @Query("SELECT p FROM RoyaltyPayment p JOIN FETCH p.period WHERE p.userId = :userId")
    List<RoyaltyPayment> findMyPayments(@Param("userId") Integer userId);
}
