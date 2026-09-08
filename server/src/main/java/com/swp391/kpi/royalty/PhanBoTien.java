package com.swp391.kpi.royalty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Chia mot so tien cho nhieu phan theo trong so, sao cho tong cac phan
 * LUON dung bang so tien ban dau - khong thua khong thieu mot dong.
 *
 * <p>Vi sao khong dung phep chia thong thuong: chia 1.000.000d cho 3 nguoi
 * deu nhau thi moi nguoi 333.333,33d. Lam tron kieu nao cung lech:
 * lam tron xuong ra tong 999.999d (thieu 1d), lam tron len ra 1.000.002d
 * (thua 2d). Voi hang nghin bai moi ky, sai so nay tich lai thanh so that
 * va bang ke se khong bao gio khop so ke toan.
 *
 * <p>Thuat toan dung o day la <b>phan du lon nhat</b> (largest remainder):
 * <ol>
 *   <li>Chia lay phan nguyen va so du cho tung phan</li>
 *   <li>Tong cac phan nguyen se thieu mot it so voi so tien ban dau</li>
 *   <li>Cong them 1 dong cho cac phan co so du lon nhat,
 *       cho den khi bu du phan thieu</li>
 *   <li>Neu so du bang nhau thi uu tien phan tu dung truoc trong danh sach
 *       - de ket qua luon tat dinh</li>
 * </ol>
 *
 * <p>Buoc 4 quan trong hon ve ngoai cua no: khong co no thi hai lan chay
 * cung du lieu co the ra hai ket qua khac nhau, va bat bien "cung input
 * -> cung output" se gay.
 *
 * <p>Toan bo phep tinh dung so nguyen ({@code divideAndRemainder}), khong
 * lam tron thap phan o buoc trung gian. Nho vay ket qua chinh xac tuyet doi
 * du so tien lon den dau.
 *
 * <p><b>Ham nay duoc dung o hai cho khac nhau:</b>
 * <ul>
 *   <li>Chia tien mot bai cho cac tac gia theo ty le %</li>
 *   <li>Ap he so tran quy: phan bo dung bang quy cho toan bo nguoi nhan,
 *       voi trong so la so tien truoc dieu chinh cua tung nguoi</li>
 * </ul>
 *
 * <p>Lop nay khong phu thuoc Spring va khong doc database. Giu nguyen
 * nhu vay de test chay duoc trong mili giay va chay duoc hang nghin case.
 */
public final class PhanBoTien {

    private PhanBoTien() {
    }

    /**
     * Phan bo {@code tong} cho cac phan theo {@code trongSo}.
     *
     * @param tong    so tien can chia het, phai >= 0 va la so nguyen dong
     * @param trongSo trong so tung phan, khong am, tong phai > 0.
     *                Co the la ty le % (60, 40) hoac so tien (1193400, 795600)
     *                - ham tu chuan hoa nen khong can cong lai thanh 100.
     * @return danh sach so tien nguyen dong, cung thu tu voi {@code trongSo},
     *         va tong dung bang {@code tong}
     */
    public static List<BigDecimal> phanBo(BigDecimal tong, List<BigDecimal> trongSo) {
        if (tong == null) {
            throw new IllegalArgumentException("tong khong duoc null");
        }
        if (tong.signum() < 0) {
            throw new IllegalArgumentException("tong khong duoc am: " + tong);
        }
        // Tien luon la so nguyen dong. Chan som de loi bao ro rang.
        if (tong.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(
                    "tong phai la so nguyen dong, nhan duoc: " + tong);
        }
        if (trongSo == null || trongSo.isEmpty()) {
            throw new IllegalArgumentException("danh sach trong so rong");
        }

        BigDecimal tongTrongSo = BigDecimal.ZERO;
        for (BigDecimal ts : trongSo) {
            if (ts == null || ts.signum() < 0) {
                throw new IllegalArgumentException("trong so khong hop le: " + ts);
            }
            tongTrongSo = tongTrongSo.add(ts);
        }
        if (tongTrongSo.signum() == 0) {
            throw new IllegalArgumentException("tong trong so phai lon hon 0");
        }

        int n = trongSo.size();
        List<BigDecimal> ketQua = new ArrayList<>(n);
        List<PhanDu> danhSachPhanDu = new ArrayList<>(n);
        BigDecimal daPhanBo = BigDecimal.ZERO;

        // Buoc 1 + 2: chia lay phan nguyen va so du.
        // Moi so du deu co cung mau so (tongTrongSo) nen so sanh truc tiep
        // duoc, khong can quy ve phan thap phan.
        for (int i = 0; i < n; i++) {
            BigDecimal tich = tong.multiply(trongSo.get(i));
            BigDecimal[] chia = tich.divideAndRemainder(tongTrongSo);

            BigDecimal phanNguyen = chia[0].setScale(0, RoundingMode.UNNECESSARY);

            ketQua.add(phanNguyen);
            danhSachPhanDu.add(new PhanDu(i, chia[1]));
            daPhanBo = daPhanBo.add(phanNguyen);
        }

        // Buoc 3: bu phan con thieu, moi lan 1 dong.
        // Luon nam trong khoang [0, n-1] vi moi phan chi thieu duoi 1 dong.
        int soDongThieu = tong.subtract(daPhanBo).intValueExact();

        // Buoc 4: uu tien so du lon nhat; bang nhau thi uu tien index nho hon
        danhSachPhanDu.sort(
                Comparator.comparing(PhanDu::soDu, Comparator.reverseOrder())
                        .thenComparingInt(PhanDu::index));

        for (int i = 0; i < soDongThieu; i++) {
            int idx = danhSachPhanDu.get(i).index();
            ketQua.set(idx, ketQua.get(idx).add(BigDecimal.ONE));
        }

        return ketQua;
    }

    private record PhanDu(int index, BigDecimal soDu) {
    }
}
