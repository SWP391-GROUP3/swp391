package com.swp391.kpi.article.service;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Tinh ma bam de kiem trung anh/video (UC-07, BR-IN-05): SHA-256 (trung y het), dHash (anh gan giong),
 * khoang cach Hamming giua 2 dHash, chuan hoa link video (nhom chot 01/11, A3).
 * Logic tinh thuan, khong dung Spring/DB (AGENTS.md muc 3.9) -> co unit test rieng.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   28  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *  ---- Ham public (controller / luong khac goi) ----
 *   32  sha256Hex()                     SHA-256 cua mot day byte, dang 64 ky tu hex (cot file_hash)
 *   43  normalizeVideoLink()            Chuan hoa link video truoc khi bam (A3): bo khoang trang, chu thuong, bo dau "/" cuoi
 *   52  videoLinkHash()                 SHA-256 cua link video da chuan hoa (A3)
 *   57  dHash()                         dHash 64 bit cua anh, dang 16 ky tu hex (cot dhash)
 *  104  hammingDistance()               So bit khac nhau giua 2 dHash (0-64); cang nho cang giong
 *  ---- Ham phu (private, chi dung trong file) ----
 *   83  averageBrightness()             Do sang trung binh (0-255) cua o thu (cx, cy) khi chia anh thanh 9 x 8 o
 */
public class ArticleMediaHasher {

    // dHash: thu anh ve 9 x 8 diem xam, so sanh 8 cap diem canh nhau tren moi hang -> 8 x 8 = 64 bit
    private static final int DHASH_WIDTH = 9;
    private static final int DHASH_HEIGHT = 8;

    // SHA-256 cua mot day byte, dang 64 ky tu hex (cot file_hash)
    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            // Java nao cung co SHA-256, khong xay ra
            throw new IllegalStateException("Máy chủ không hỗ trợ SHA-256.", e);
        }
    }

    // Chuan hoa link video truoc khi bam (A3): bo khoang trang, chu thuong, bo dau "/" cuoi
    public static String normalizeVideoLink(String link) {
        String result = link.trim().toLowerCase();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    // SHA-256 cua link video da chuan hoa (A3)
    public static String videoLinkHash(String link) {
        return sha256Hex(normalizeVideoLink(link).getBytes(StandardCharsets.UTF_8));
    }

    // dHash 64 bit cua anh, dang 16 ky tu hex (cot dhash)
    public static String dHash(BufferedImage image) {
        // 1. Chia anh thanh 9 x 8 o, moi o lay do sang trung binh (bo mau, bo kich thuoc goc).
        //    Lay trung binh ca o (khong lay 1 diem) de anh thu nho / phong to van ra gan cung ket qua
        int[][] cells = new int[DHASH_HEIGHT][DHASH_WIDTH];
        for (int cy = 0; cy < DHASH_HEIGHT; cy++) {
            for (int cx = 0; cx < DHASH_WIDTH; cx++) {
                cells[cy][cx] = averageBrightness(image, cx, cy);
            }
        }

        // 2. Moi hang: o ben trai sang hon o ben phai -> bit 1, nguoc lai bit 0
        long bits = 0;
        for (int y = 0; y < DHASH_HEIGHT; y++) {
            for (int x = 0; x < DHASH_WIDTH - 1; x++) {
                bits = bits << 1;
                if (cells[y][x] > cells[y][x + 1]) {
                    bits = bits | 1;
                }
            }
        }

        // 3. 64 bit -> 16 ky tu hex (du 16 ky tu, them so 0 o dau)
        return String.format("%016x", bits);
    }

    // Do sang trung binh (0-255) cua o thu (cx, cy) khi chia anh thanh 9 x 8 o
    private static int averageBrightness(BufferedImage image, int cx, int cy) {
        int x0 = cx * image.getWidth() / DHASH_WIDTH;
        int x1 = Math.max(x0 + 1, (cx + 1) * image.getWidth() / DHASH_WIDTH);
        int y0 = cy * image.getHeight() / DHASH_HEIGHT;
        int y1 = Math.max(y0 + 1, (cy + 1) * image.getHeight() / DHASH_HEIGHT);

        long sum = 0;
        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xff;
                int green = (rgb >> 8) & 0xff;
                int blue = rgb & 0xff;
                // Doi mau sang do sang theo cong thuc anh xam thuong dung (mat nguoi nhay voi mau xanh la nhat)
                sum += (red * 299 + green * 587 + blue * 114) / 1000;
            }
        }
        return (int) (sum / ((long) (x1 - x0) * (y1 - y0)));
    }

    // So bit khac nhau giua 2 dHash (0-64); cang nho cang giong
    public static int hammingDistance(String dHash1, String dHash2) {
        long a = Long.parseUnsignedLong(dHash1, 16);
        long b = Long.parseUnsignedLong(dHash2, 16);
        return Long.bitCount(a ^ b);
    }
}
