package com.swp391.kpi.service;

import com.swp391.kpi.entity.AppealResolutionRule;
import com.swp391.kpi.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;

/**
 * Logic thuan (khong @Service) so sanh diem cham lai voi diem goc va tinh tong co trong so, dung khi
 * chot khieu nai.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   26  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *  ---- Ham public (controller / luong khac goi) ----
 *   29  Item()                          item
 *   30  Result()                        result
 *   32  compare()                       compare
 *   46  weightedTotal()                 weighted total
 *  ---- Ham phu (private, chi dung trong file) ----
 *   70  requireScore()                  bat buoc score
 *   76  result()                        result
 */
public class AppealScoreCalculator {
    private static final BigDecimal MAX_SCORE = new BigDecimal("100");
    private static final BigDecimal COMMENT_THRESHOLD = new BigDecimal("50");
    private static final BigDecimal TWO = new BigDecimal("2");
    public record Item(Integer criterionId, BigDecimal weight, BigDecimal score, String comment) {}
    public record Result(boolean requiresSecondReview, BigDecimal effectiveScore, AppealResolutionRule rule) {}

    public Result compare(BigDecimal s0, BigDecimal s1, BigDecimal s2, BigDecimal threshold) { // [LUONG 4 - SO SANH & CHOT] buoc 7/7 cong thuc: |S1-S0|<=T lay S1; lech hon thi cho S2; co S2 thi trung binh voi diem GAN S2 hon (bang nhau lay S2)
        requireScore(s0); requireScore(s1); requireScore(threshold);
        if (s1.subtract(s0).abs().compareTo(threshold) <= 0) {
            if (s2 != null) throw new BusinessException("Không được chấm S2 khi chênh lệch chưa vượt ngưỡng.");
            return result(s1, AppealResolutionRule.USE_S1);
        }
        if (s2 == null) return new Result(true, null, null);
        requireScore(s2);
        int distance = s2.subtract(s0).abs().compareTo(s2.subtract(s1).abs());
        if (distance < 0) return result(s0.add(s2).divide(TWO), AppealResolutionRule.AVERAGE_S0_S2);
        if (distance > 0) return result(s1.add(s2).divide(TWO), AppealResolutionRule.AVERAGE_S1_S2);
        return result(s2, AppealResolutionRule.USE_S2_TIE);
    }

    public BigDecimal weightedTotal(List<Item> items, boolean complete) { // [LUONG 3 - CHAM MU] buoc 7/7 cong thuc: tong = cong(diem * trong so / 100), trong so tong phai = 100%, diem < 50 phai co nhan xet
        if (items == null || items.isEmpty()) throw new BusinessException("Bộ tiêu chí không có dữ liệu.");
        var ids = new HashSet<Integer>();
        BigDecimal weight = BigDecimal.ZERO, total = BigDecimal.ZERO;
        boolean missing = false;
        for (var item : items) {
            if (item.criterionId() == null || !ids.add(item.criterionId()) || item.weight() == null
                    || item.weight().signum() <= 0 || item.weight().compareTo(MAX_SCORE) > 0)
                throw new BusinessException("Tiêu chí hoặc trọng số không hợp lệ.");
            weight = weight.add(item.weight());
            if (item.comment() != null && item.comment().length() > 1000)
                throw new BusinessException("Nhận xét không được vượt quá 1000 ký tự.");
            if (item.score() == null) { missing = true; continue; }
            requireScore(item.score());
            if (complete && item.score().compareTo(COMMENT_THRESHOLD) < 0
                    && (item.comment() == null || item.comment().isBlank()))
                throw new BusinessException("Phải nhập nhận xét cho mọi tiêu chí có điểm dưới 50.");
            total = total.add(item.score().multiply(item.weight()).divide(MAX_SCORE));
        }
        if (weight.compareTo(MAX_SCORE) != 0) throw new BusinessException("Tổng trọng số bộ tiêu chí phải bằng 100%.");
        if (missing && complete) throw new BusinessException("Phải chấm đủ mọi tiêu chí trước khi nộp.");
        return missing ? null : total.setScale(2, RoundingMode.HALF_UP);
    }

    private void requireScore(BigDecimal score) {
        if (score == null || score.signum() < 0 || score.compareTo(MAX_SCORE) > 0
                || score.stripTrailingZeros().scale() > 2)
            throw new BusinessException("Điểm và ngưỡng phải từ 0 đến 100, tối đa hai chữ số thập phân.");
    }

    private Result result(BigDecimal score, AppealResolutionRule rule) {
        return new Result(false, score.setScale(2, RoundingMode.HALF_UP), rule);
    }
}
