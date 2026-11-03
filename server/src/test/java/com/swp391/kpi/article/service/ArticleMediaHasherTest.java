package com.swp391.kpi.article.service;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiem tra ArticleMediaHasher (UC-07, BR-IN-05). Class tinh thuan nen test chay
 * khong can Spring, khong can DB. Anh thu tao bang code, khong can file.
 */
class ArticleMediaHasherTest {

    // Anh rong x cao, sang dan tu trai (den) sang phai (trang) hoac nguoc lai
    private static BufferedImage gradient(int width, int height, boolean brightOnLeft) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            int level = x * 255 / (width - 1);
            if (brightOnLeft) {
                level = 255 - level;
            }
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, new Color(level, level, level).getRGB());
            }
        }
        return image;
    }

    private static BufferedImage solid(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    @Test
    void sha256OfAbcMatchesKnownValue() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                ArticleMediaHasher.sha256Hex("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void videoLinkIsNormalizedBeforeHashing() {
        assertEquals("https://video.vn/clip-1", ArticleMediaHasher.normalizeVideoLink("  HTTPS://Video.vn/Clip-1//  "));
        assertEquals(ArticleMediaHasher.videoLinkHash("https://video.vn/clip-1"),
                ArticleMediaHasher.videoLinkHash("HTTPS://VIDEO.VN/CLIP-1/"));
        assertNotEquals(ArticleMediaHasher.videoLinkHash("https://video.vn/clip-1"),
                ArticleMediaHasher.videoLinkHash("https://video.vn/clip-2"));
    }

    @Test
    void dHashOfUniformImageIsAllZeros() {
        assertEquals("0000000000000000", ArticleMediaHasher.dHash(solid(100, 80, Color.GRAY)));
    }

    @Test
    void dHashOfImageDarkeningToTheRightIsAllOnes() {
        assertEquals("ffffffffffffffff", ArticleMediaHasher.dHash(gradient(90, 80, true)));
    }

    @Test
    void resizedCopyHasSameOrVeryCloseDHash() {
        String original = ArticleMediaHasher.dHash(gradient(900, 800, true));
        String resized = ArticleMediaHasher.dHash(gradient(180, 160, true));
        assertTrue(ArticleMediaHasher.hammingDistance(original, resized) <= 5);
    }

    @Test
    void halfSizeCopyOfDetailedImageIsStillSimilar() {
        // Anh co hoa van (song sin), ban thu nho 1/2 -> lech it bit (<= 5 = DHASH_MAX_BITS mac dinh)
        BufferedImage large = new BufferedImage(240, 180, BufferedImage.TYPE_INT_RGB);
        BufferedImage small = new BufferedImage(120, 90, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 240; x++) {
            for (int y = 0; y < 180; y++) {
                int level = (int) (128 + 100 * Math.sin((x + 37) / 15.0) * Math.cos((y + 37) / 20.0));
                large.setRGB(x, y, new Color(level, level, level).getRGB());
            }
        }
        for (int x = 0; x < 120; x++) {
            for (int y = 0; y < 90; y++) {
                int level = (int) (128 + 100 * Math.sin((x * 2 + 37) / 15.0) * Math.cos((y * 2 + 37) / 20.0));
                small.setRGB(x, y, new Color(level, level, level).getRGB());
            }
        }
        int distance = ArticleMediaHasher.hammingDistance(ArticleMediaHasher.dHash(large), ArticleMediaHasher.dHash(small));
        assertTrue(distance <= 5, "lech " + distance + " bit");
    }

    @Test
    void hammingDistanceCountsDifferentBits() {
        assertEquals(0, ArticleMediaHasher.hammingDistance("c3a1e0f07b1d2c4e", "c3a1e0f07b1d2c4e"));
        assertEquals(1, ArticleMediaHasher.hammingDistance("0000000000000000", "0000000000000001"));
        assertEquals(64, ArticleMediaHasher.hammingDistance("0000000000000000", "ffffffffffffffff"));
    }
}
