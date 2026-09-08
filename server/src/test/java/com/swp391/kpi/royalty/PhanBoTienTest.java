package com.swp391.kpi.royalty;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cho thuat toan chia tien.
 *
 * <p>Diem dang chu y: ngoai vai case cu the, o day co ca test chay tren
 * HANG NGHIN bo du lieu ngau nhien de khang dinh cac <b>bat bien</b> luon
 * dung. Cach nay bat duoc nhieu loi hon la ngoi liet ke case bang tay,
 * vi no thu ca nhung to hop ma khong ai nghi ra.
 *
 * <p>Test khong can database, khong can Spring context -> chay het trong
 * duoi mot giay.
 */
class PhanBoTienTest {

    private static BigDecimal vnd(String s) {
        return new BigDecimal(s);
    }

    private static List<BigDecimal> trongSo(String... values) {
        List<BigDecimal> ds = new ArrayList<>();
        for (String v : values) {
            ds.add(new BigDecimal(v));
        }
        return ds;
    }

    private static BigDecimal tongCua(List<BigDecimal> ds) {
        BigDecimal t = BigDecimal.ZERO;
        for (BigDecimal x : ds) {
            t = t.add(x);
        }
        return t;
    }

    // =======================================================================
    // Case cu the
    // =======================================================================

    @Test
    @DisplayName("Chia deu cho 3 nguoi: phan du 1 dong ve nguoi dau tien")
    void chiaDeuChoBaNguoi() {
        List<BigDecimal> kq = PhanBoTien.phanBo(vnd("1000000"), trongSo("1", "1", "1"));

        // 1.000.000 / 3 = 333.333,33 -> lam tron xuong ca 3 la 999.999, thieu 1 dong
        assertEquals(vnd("333334"), kq.get(0));
        assertEquals(vnd("333333"), kq.get(1));
        assertEquals(vnd("333333"), kq.get(2));
        assertEquals(vnd("1000000"), tongCua(kq));
    }

    @Test
    @DisplayName("Chia tien bai cho dong tac gia theo ty le 60/40")
    void chiaChoDongTacGia() {
        // Bai phong su dieu tra, loai A, trang nhat:
        //   150.000 x 10 x 1.3 x 1.2 = 2.340.000d
        // Tru thu lao BTV 10% va hoa si 5% -> con 1.989.000d cho cac tac gia
        List<BigDecimal> kq = PhanBoTien.phanBo(vnd("1989000"), trongSo("60", "40"));

        assertEquals(vnd("1193400"), kq.get(0));
        assertEquals(vnd("795600"), kq.get(1));
        assertEquals(vnd("1989000"), tongCua(kq));
    }

    @Test
    @DisplayName("Chia ca bai cho 4 vai tro: tong luon bang tien bai")
    void chiaChoBonVaiTro() {
        // BTV 10%, hoa si 5%, tac gia chinh 51%, dong tac gia 34%
        List<BigDecimal> kq = PhanBoTien.phanBo(vnd("2340000"), trongSo("10", "5", "51", "34"));

        assertEquals(vnd("2340000"), tongCua(kq), "tong chia phai bang dung tien bai");
    }

    @Test
    @DisplayName("Ap he so tran quy: tong sau cat giam bang dung quy")
    void apHeSoTranQuy() {
        // Tong tinh ra 520 trieu nhung quy chi co 480 trieu.
        // Dung chinh ham nay, voi trong so la so tien truoc dieu chinh.
        List<BigDecimal> tienTruoc = trongSo("120000000", "200000000", "150000000", "50000000");

        List<BigDecimal> kq = PhanBoTien.phanBo(vnd("480000000"), tienTruoc);

        assertEquals(vnd("480000000"), tongCua(kq), "tong chi phai bang dung quy");
        for (BigDecimal x : kq) {
            assertTrue(x.signum() >= 0, "khong ai duoc nhan so am");
        }
    }

    @Test
    @DisplayName("Nguoi co trong so 0 thi nhan 0 dong")
    void trongSoBangKhong() {
        List<BigDecimal> kq = PhanBoTien.phanBo(vnd("1000000"), trongSo("50", "50", "0"));

        assertEquals(vnd("0"), kq.get(2));
        assertEquals(vnd("1000000"), tongCua(kq));
    }

    // =======================================================================
    // Bat bien - chay tren du lieu ngau nhien
    // =======================================================================

    @Test
    @DisplayName("BAT BIEN 1: tong cac phan luon bang dung so tien ban dau")
    void batBien_tongLuonKhop() {
        // Seed co dinh de khi test fail thi chay lai tai hien duoc y het.
        Random rnd = new Random(20260908L);

        for (int lan = 0; lan < 5000; lan++) {
            BigDecimal tong = BigDecimal.valueOf(rnd.nextInt(500_000_000) + 1L);

            int soPhan = rnd.nextInt(12) + 1;
            List<BigDecimal> ts = new ArrayList<>(soPhan);
            for (int i = 0; i < soPhan; i++) {
                ts.add(BigDecimal.valueOf(rnd.nextInt(1000) + 1L));
            }

            List<BigDecimal> kq = PhanBoTien.phanBo(tong, ts);

            assertEquals(tong, tongCua(kq),
                    "Lech tien o lan thu " + lan + " voi tong=" + tong + " trongSo=" + ts);
        }
    }

    @Test
    @DisplayName("BAT BIEN 2: khong ai nhan so am")
    void batBien_khongAiAm() {
        Random rnd = new Random(11111L);

        for (int lan = 0; lan < 3000; lan++) {
            BigDecimal tong = BigDecimal.valueOf(rnd.nextInt(100_000_000));

            int soPhan = rnd.nextInt(10) + 1;
            List<BigDecimal> ts = new ArrayList<>(soPhan);
            for (int i = 0; i < soPhan; i++) {
                ts.add(BigDecimal.valueOf(rnd.nextInt(500)));
            }
            // dam bao tong trong so > 0
            ts.set(rnd.nextInt(soPhan), BigDecimal.ONE);

            for (BigDecimal x : PhanBoTien.phanBo(tong, ts)) {
                assertTrue(x.signum() >= 0, "Xuat hien so am o lan thu " + lan);
            }
        }
    }

    @Test
    @DisplayName("BAT BIEN 3: cung input luon ra cung output")
    void batBien_tatDinh() {
        Random rnd = new Random(777L);

        for (int lan = 0; lan < 1000; lan++) {
            BigDecimal tong = BigDecimal.valueOf(rnd.nextInt(50_000_000) + 1L);

            int soPhan = rnd.nextInt(8) + 2;
            List<BigDecimal> ts = new ArrayList<>(soPhan);
            for (int i = 0; i < soPhan; i++) {
                ts.add(BigDecimal.valueOf(rnd.nextInt(100) + 1L));
            }

            assertEquals(PhanBoTien.phanBo(tong, ts), PhanBoTien.phanBo(tong, ts),
                    "Ket qua khong tat dinh o lan thu " + lan);
        }
    }

    @Test
    @DisplayName("BAT BIEN 4: chenh lech giua hai nguoi cung trong so toi da 1 dong")
    void batBien_congBang() {
        Random rnd = new Random(2468L);

        for (int lan = 0; lan < 2000; lan++) {
            BigDecimal tong = BigDecimal.valueOf(rnd.nextInt(10_000_000) + 1L);
            int soPhan = rnd.nextInt(9) + 2;

            // tat ca cung trong so -> chia deu
            List<BigDecimal> ts = new ArrayList<>(soPhan);
            for (int i = 0; i < soPhan; i++) {
                ts.add(BigDecimal.ONE);
            }

            List<BigDecimal> kq = PhanBoTien.phanBo(tong, ts);
            BigDecimal min = kq.get(0);
            BigDecimal max = kq.get(0);
            for (BigDecimal x : kq) {
                min = min.min(x);
                max = max.max(x);
            }

            assertTrue(max.subtract(min).compareTo(BigDecimal.ONE) <= 0,
                    "Chia deu ma lech qua 1 dong o lan thu " + lan);
        }
    }
}
