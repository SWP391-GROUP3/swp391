package com.swp391.kpi.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Kiem tra tien thuc chi va no chuyen ky (UC-53 A2, GBR-12), khong can Spring/DB.
 * Moi test kiem them dang thuc cua CHECK ck_royalty_payments_amount: paid - debtOut = gross - debtIn.
 */
class RoyaltyPaymentCalculatorTest {

    @Test
    void noDebtPaysFullGross() {
        assertPayment("966000", "0", "966000", "0");
    }

    @Test
    void debtSmallerThanGrossIsDeductedFully() {
        // Ky truoc no 200.000, ky nay duoc 500.000 -> chi 300.000, het no
        assertPayment("500000", "200000", "300000", "0");
    }

    @Test
    void debtEqualToGrossPaysZeroAndClearsDebt() {
        assertPayment("200000", "200000", "0", "0");
    }

    @Test
    void debtLargerThanGrossPaysZeroAndCarriesTheRest() {
        // No 300.000, ky nay chi duoc 100.000 -> phieu 0d, con no 200.000 sang ky sau
        assertPayment("100000", "300000", "0", "200000");
    }

    @Test
    void personWithDebtButNoLinesKeepsWholeDebt() {
        // Phieu 0d cho nguoi con no ma ky nay khong co dong tien nao (nhom chot 27/10/2026)
        assertPayment("0", "50000", "0", "50000");
    }

    @Test
    void negativeGrossFromAdjustmentBecomesDebt() {
        // Dieu chinh am 150.000 (BR-RY-06), khong no cu -> phieu 0d, no 150.000 (UC-53 A2)
        assertPayment("-150000", "0", "0", "150000");
    }

    private static void assertPayment(String gross, String debtIn, String expectedPaid, String expectedDebtOut) {
        BigDecimal grossAmount = new BigDecimal(gross);
        BigDecimal debtInAmount = new BigDecimal(debtIn);

        BigDecimal paid = RoyaltyPaymentCalculator.paidAmount(grossAmount, debtInAmount);
        BigDecimal debtOut = RoyaltyPaymentCalculator.debtOut(grossAmount, debtInAmount);

        assertMoney(expectedPaid, paid);
        assertMoney(expectedDebtOut, debtOut);
        assertEquals(0, paid.subtract(debtOut).compareTo(grossAmount.subtract(debtInAmount)),
                "paid - debtOut phai bang gross - debtIn");
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "mong doi " + expected + " nhung la " + actual);
    }
}
