package com.swp391.kpi.service;

import com.swp391.kpi.dto.ArticlePublishedResponse;
import com.swp391.kpi.dto.RoyaltyPendingArticleResponse;
import com.swp391.kpi.exception.BusinessException;
import com.swp391.kpi.repository.RoyaltyGradingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cham nhuan but (Thu ky toa soan): danh sach bai cho cham (UC-46).
 */
@Service
@RequiredArgsConstructor
public class RoyaltyGradingService {

    private static final String ROYALTY_FLOW = "BF-04";  // gia tri cot system_parameters.flow cua luong nhuan but

    private final RoyaltyGradingRepository royaltyGradingRepository;
    private final ArticlePublishedService articlePublishedService;  // service cua BF-01, khong query bang articles
    private final SystemParameterService systemParameterService;

    // UC-46: bai da dang ma chua co phieu cham, kem han chi va canh bao sap toi han.
    // Thu tu giu nguyen nhu BF-01 tra ve: bai dang som nhat (gan han nhat) len dau.
    @Transactional(readOnly = true)
    public List<RoyaltyPendingArticleResponse> getPendingArticles() {
        Map<String, String> params = systemParameterService.getValuesByFlow(ROYALTY_FLOW);
        int deadlineDays = readInt(params, "PAYMENT_DEADLINE_DAYS");  // han chi tinh tu ngay dang
        int warnDays = readInt(params, "PAYMENT_WARN_DAYS");          // con <= so ngay nay thi canh bao

        // Doi sang Set de kiem "bai da cham chua" nhanh, khong phai duyet ca danh sach moi lan
        Set<Integer> gradedArticleIds = new HashSet<>(royaltyGradingRepository.findGradedArticleIds());
        LocalDate today = LocalDate.now();

        List<RoyaltyPendingArticleResponse> result = new ArrayList<>();
        for (ArticlePublishedResponse article : articlePublishedService.getPublishedArticles()) {
            if (gradedArticleIds.contains(article.getId())) {
                continue;  // da co phieu cham -> khong con cho cham
            }
            LocalDate deadline = article.getPublishedAt().toLocalDate().plusDays(deadlineDays);
            long daysLeft = ChronoUnit.DAYS.between(today, deadline);  // am = da qua han
            boolean nearDeadline = daysLeft <= warnDays;
            result.add(RoyaltyPendingArticleResponse.from(article, deadline, daysLeft, nearDeadline));
        }
        return result;
    }

    // Lay 1 tham so so nguyen. Thieu hoac sai dinh dang thi bao loi ro rang,
    // khong tu dung so mac dinh (AGENTS.md muc 5: khong viet cung so trong code).
    private static int readInt(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null) {
            throw new BusinessException("Thiếu tham số hệ thống " + key + ", vui lòng báo Quản trị viên bổ sung.");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException("Tham số hệ thống " + key + " không phải là số nguyên: " + value);
        }
    }
}
