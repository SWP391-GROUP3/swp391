package com.swp391.kpi.kpi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Bo tinh toan thuan cho bao cao su kien xu huong va phat hien tang dot bien (UC-65).
 * Logic nghiep vu: Phat hien su kien co luot xem trong 7 ngay gan nhat vuot qua 2x muc trung binh 4 tuan truoc do.
 * Khong doc DB, khong co @Service (AGENTS.md muc 1, 3.1, 3.9).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   23  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   33  calculateRecent7DaysViews()     Tinh tong luot xem trong 7 ngay gan nhat [anchorDate - 6 ngay, anchorDate]
 *   60  calculateBaseline4WeeksAverage()  Tinh luot xem trung binh 7 ngay cua 4 tuan co so truoc do
 *   89  isSurgeDetected()               Kiem tra su kien co dang tang dot bien khong: Luot xem trong 7 ngay gan nhat vuot qua 2x luot xem trung binh …
 *  110  calculateSurgeRatio()           Tinh he so tang truong so voi muc co so (so lan)
 */
public class TrendingEventCalculator {

    public static final int DEFAULT_DECIMAL_SCALE = 2;
    public static final double SURGE_THRESHOLD_MULTIPLIER = 2.0;

    /**
     * Tinh tong luot xem trong 7 ngay gan nhat [anchorDate - 6 ngay, anchorDate].
     *
     * @param dailyViewsMap Ban do anh xa ngay -> so luot xem trong ngay
     * @param anchorDate    Ngay moc danh gia (T)
     * @return Tong luot xem 7 ngay gan nhat
     */
    public long calculateRecent7DaysViews(Map<LocalDate, Long> dailyViewsMap, LocalDate anchorDate) {
        if (dailyViewsMap == null || dailyViewsMap.isEmpty() || anchorDate == null) {
            return 0L;
        }

        long totalViews = 0L;
        LocalDate startDate = anchorDate.minusDays(6);

        for (LocalDate d = startDate; !d.isAfter(anchorDate); d = d.plusDays(1)) {
            Long v = dailyViewsMap.get(d);
            if (v != null && v > 0) {
                totalViews += v;
            }
        }

        return totalViews;
    }

    /**
     * Tinh luot xem trung binh 7 ngay cua 4 tuan co so truoc do.
     * Cua so 4 tuan truoc: tu [anchorDate - 34 ngay] den [anchorDate - 7 ngay] (tong cong 28 ngay = 4 tuan).
     * Luot xem trung binh moi tuan (7 ngay) = Tong luot xem 28 ngay / 4.
     *
     * @param dailyViewsMap Ban do anh xa ngay -> so luot xem trong ngay
     * @param anchorDate    Ngay moc danh gia (T)
     * @return Luot xem trung binh 7 ngay cua 4 tuan truoc
     */
    public BigDecimal calculateBaseline4WeeksAverage(Map<LocalDate, Long> dailyViewsMap, LocalDate anchorDate) {
        if (dailyViewsMap == null || dailyViewsMap.isEmpty() || anchorDate == null) {
            return BigDecimal.ZERO.setScale(DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
        }

        long totalBaselineViews = 0L;
        LocalDate baselineStart = anchorDate.minusDays(34); // 7 ngay gan nhat + 28 ngay truoc do - 1 = 34 ngay truoc
        LocalDate baselineEnd = anchorDate.minusDays(7);

        for (LocalDate d = baselineStart; !d.isAfter(baselineEnd); d = d.plusDays(1)) {
            Long v = dailyViewsMap.get(d);
            if (v != null && v > 0) {
                totalBaselineViews += v;
            }
        }

        return BigDecimal.valueOf(totalBaselineViews)
                .divide(BigDecimal.valueOf(4), DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Kiem tra su kien co dang tang dot bien khong:
     * Luot xem trong 7 ngay gan nhat vuot qua 2x luot xem trung binh 7 ngay cua 4 tuan truoc.
     * Truong hop baseline = 0 va luot xem 7 ngay > 0 thi cung duoc coi la dot bien moi xuat hien.
     *
     * @param recent7DaysViews Luot xem 7 ngay gan nhat
     * @param baselineAverage  Luot xem trung binh 7 ngay cua 4 tuan truoc
     * @return true neu thoa man dieu kien dot bien
     */
    public boolean isSurgeDetected(long recent7DaysViews, BigDecimal baselineAverage) {
        if (recent7DaysViews <= 0) {
            return false;
        }

        if (baselineAverage == null || baselineAverage.compareTo(BigDecimal.ZERO) == 0) {
            // Neu truoc do khong co view ma 7 ngay gan nhat co view -> dot bien moi
            return true;
        }

        BigDecimal threshold = baselineAverage.multiply(BigDecimal.valueOf(SURGE_THRESHOLD_MULTIPLIER));
        return BigDecimal.valueOf(recent7DaysViews).compareTo(threshold) > 0;
    }

    /**
     * Tinh he so tang truong so voi muc co so (so lan).
     *
     * @param recent7DaysViews Luot xem 7 ngay gan nhat
     * @param baselineAverage  Luot xem trung binh 7 ngay cua 4 tuan truoc
     * @return He so tang truong (vd: 2.50 lan). Neu baseline = 0 thi tra ve 0.00 (hoac 999 neu co views)
     */
    public BigDecimal calculateSurgeRatio(long recent7DaysViews, BigDecimal baselineAverage) {
        if (recent7DaysViews <= 0) {
            return BigDecimal.ZERO.setScale(DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
        }

        if (baselineAverage == null || baselineAverage.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(999.00).setScale(DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(recent7DaysViews)
                .divide(baselineAverage, DEFAULT_DECIMAL_SCALE, RoundingMode.HALF_UP);
    }
}
