package com.swp391.kpi.service;

import com.swp391.kpi.dto.CriteriaScoreInput;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;


// Tinh diem chat luong DECIMAL(5,2), lam tron HALF_UP theo BR-SC-07

/**
 * Engine tính điểm chất lượng bài viết (Quality Scoring Calculator).
 * Tuân thủ quy tắc làm tròn DECIMAL(5,2), RoundingMode.HALF_UP theo AGENTS.md và GBR-08.
 *
 * MUC LUC (so dong)
 *   23  hang so                      - hang so dung trong file
 *   32  calculateTotalScore()        - Tính tổng điểm có trọng số từ danh sách điểm thành phần.
 *   56  validateItem()               - Kiểm tra tính hợp lệ của điểm thành phần (0 - 100).
 *   78  isCommentRequired()          - Kiểm tra tiêu chí có bắt buộc nhập nhận xét hay không (BR-QA-08: score …
 */
@Component

public class ScoreCalculator {

    public static final BigDecimal MIN_SCORE = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    public static final BigDecimal MAX_SCORE = new BigDecimal("100.00");
    public static final BigDecimal COMMENT_REQUIRED_THRESHOLD = new BigDecimal("50.00");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    /**
     * Tính tổng điểm có trọng số từ danh sách điểm thành phần.
     * Công thức: Total = SUM(Score_i * Weight_i / 100), làm tròn HALF_UP 2 chữ số thập phân.
     */
    public BigDecimal calculateTotalScore(List<CriteriaScoreInput> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Danh sách điểm thành phần không được rỗng");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CriteriaScoreInput item : items) {
            validateItem(item);

            // weightedScore = score * (weight / 100)
            BigDecimal weightedContribution = item.getScore()
                    .multiply(item.getWeight())
                    .divide(ONE_HUNDRED, 4, RoundingMode.HALF_UP);

            total = total.add(weightedContribution);
        }

        // Làm tròn cuối cùng về 2 chữ số thập phân (DECIMAL(5,2))
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Kiểm tra tính hợp lệ của điểm thành phần (0 - 100).
     */
    public void validateItem(CriteriaScoreInput item) {
        if (item.getScore() == null) {
            throw new IllegalArgumentException("Điểm của tiêu chí không được để trống");
        }
        if (item.getWeight() == null || item.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Trọng số của tiêu chí phải lớn hơn 0");
        }
        if (item.getScore().compareTo(MIN_SCORE) < 0 || item.getScore().compareTo(MAX_SCORE) > 0) {
            throw new IllegalArgumentException("Điểm tiêu chí phải nằm trong khoảng 0 đến 100 (Hiện tại: " + item.getScore() + ")");
        }

        // BR-QA-08: Nếu điểm < 50 thì bắt buộc phải có nhận xét
        if (isCommentRequired(item.getScore())) {
            if (item.getComment() == null || item.getComment().trim().isEmpty()) {
                throw new IllegalArgumentException("Tiêu chí đạt dưới 50 điểm bắt buộc phải nhập nhận xét lý do!");
            }
        }
    }

    /**
     * Kiểm tra tiêu chí có bắt buộc nhập nhận xét hay không (BR-QA-08: score < 50.00).
     */
    public boolean isCommentRequired(BigDecimal score) {
        if (score == null) return false;
        return score.compareTo(COMMENT_REQUIRED_THRESHOLD) < 0;
    }
}
