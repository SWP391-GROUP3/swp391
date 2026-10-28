package com.swp391.kpi.repository;

import com.swp391.kpi.entity.RoyaltyPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Doc/ghi bang royalty_payments (phieu chi tung nguoi trong ky, UC-53, UC-54).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   17  findByPeriodId()                Cac phieu chi cua 1 ky: khoa ky sau lay no chuyen tiep (debt_out)
 */
public interface RoyaltyPaymentRepository extends JpaRepository<RoyaltyPayment, Integer> {

    // Cac phieu chi cua 1 ky: khoa ky sau lay no chuyen tiep (debt_out)
    List<RoyaltyPayment> findByPeriodId(Integer periodId);
}
