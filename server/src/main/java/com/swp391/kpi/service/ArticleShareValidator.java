package com.swp391.kpi.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Kiem danh sach tac gia + ty le cua 1 bai (BR-IN-02, GBR-01, UC-04 E1/E2).
 * Logic tinh thuan, khong dung Spring/DB (AGENTS.md muc 3.9) -> co unit test rieng.
 * So tac gia toi da truyen vao tu system_parameters (MAX_AUTHORS_PER_ARTICLE), khong viet cung.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   23  field                           repository / service duoc inject
 *   26  ArticleShareValidator()         maxAuthors = MAX_AUTHORS_PER_ARTICLE doc tu system_parameters
 *   34  findError()                     userIds.get(i) co ty le shares.get(i), danh sach gom ca tac gia chinh
 */
public class ArticleShareValidator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final int maxAuthors;

    // maxAuthors = MAX_AUTHORS_PER_ARTICLE doc tu system_parameters
    public ArticleShareValidator(int maxAuthors) {
        this.maxAuthors = maxAuthors;
    }

    /**
     * userIds.get(i) co ty le shares.get(i), danh sach gom ca tac gia chinh.
     * Hop le -> tra ve null; sai -> tra ve thong bao loi tieng Viet de service nem BusinessException.
     */
    public String findError(Integer mainAuthorId, List<Integer> userIds, List<BigDecimal> shares) {
        // E1: qua so tac gia toi da
        if (userIds.size() > maxAuthors) {
            return "Một bài có tối đa " + maxAuthors + " tác giả.";
        }

        // Moi nguoi chi xuat hien 1 lan (khoa chinh article_authors la article_id + user_id)
        Set<Integer> seen = new HashSet<>();
        for (Integer userId : userIds) {
            if (userId == null || !seen.add(userId)) {
                return "Mỗi tác giả chỉ được khai một lần.";
            }
        }

        // Dung 1 tac gia chinh va tac gia chinh phai co trong danh sach (BR-IN-02)
        if (!seen.contains(mainAuthorId)) {
            return "Danh sách tác giả phải có tác giả chính.";
        }

        // E2: moi ty le > 0% va tong dung 100%
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal share : shares) {
            if (share == null || share.compareTo(BigDecimal.ZERO) <= 0 || share.compareTo(ONE_HUNDRED) > 0) {
                return "Mỗi tác giả phải có tỷ lệ trên 0% và tổng tỷ lệ phải bằng 100%.";
            }
            total = total.add(share);
        }
        if (total.compareTo(ONE_HUNDRED) != 0) {
            return "Mỗi tác giả phải có tỷ lệ trên 0% và tổng tỷ lệ phải bằng 100% (hiện là " + total.stripTrailingZeros().toPlainString() + "%).";
        }
        return null;
    }
}
