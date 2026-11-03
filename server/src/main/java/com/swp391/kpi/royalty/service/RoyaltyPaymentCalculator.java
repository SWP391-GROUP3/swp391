package com.swp391.kpi.royalty.service;

import java.math.BigDecimal;

/**
 * Tinh tien thuc chi va no chuyen ky cua 1 phieu chi (UC-53 A2, GBR-12: phieu chi khong am).
 * Class tinh toan thuan: khong doc DB, khong co @Service (AGENTS.md muc 3.1, 3.9).
 * Cong thuc theo comment bang royalty_payments trong schema.sql:
 *   con lai = gross - debtIn
 *   con lai >= 0: paid = con lai, debtOut = 0
 *   con lai <  0: paid = 0,       debtOut = debtIn - gross
 * Luon dung: paid - debtOut = gross - debtIn (CHECK ck_royalty_payments_amount).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  paidAmount()                    So tien thuc chi cho nguoi nhan: phan con lai sau khi tru no, khong am
 *   30  debtOut()                       No chuyen sang ky sau: chi co khi tien ky nay khong du tra no
 */
public class RoyaltyPaymentCalculator {

    // So tien thuc chi cho nguoi nhan: phan con lai sau khi tru no, khong am
    public static BigDecimal paidAmount(BigDecimal grossAmount, BigDecimal debtIn) {
        BigDecimal remaining = grossAmount.subtract(debtIn);
        if (remaining.signum() >= 0) {
            return remaining;
        }
        return BigDecimal.ZERO;
    }

    // No chuyen sang ky sau: chi co khi tien ky nay khong du tra no
    public static BigDecimal debtOut(BigDecimal grossAmount, BigDecimal debtIn) {
        BigDecimal remaining = grossAmount.subtract(debtIn);
        if (remaining.signum() >= 0) {
            return BigDecimal.ZERO;
        }
        return remaining.negate();   // = debtIn - gross
    }
}
