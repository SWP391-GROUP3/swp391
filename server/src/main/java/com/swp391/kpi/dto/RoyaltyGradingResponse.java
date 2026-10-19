package com.swp391.kpi.dto;

import com.swp391.kpi.entity.RoyaltyNature;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Phieu cham nhuan but 1 bai (UC-47, UC-48), du tung buoc cong thuc de Thu ky xem.
 * Dung cho ca XEM TRUOC (gradingId = null, chua luu) va sau khi HOAN TAT (co gradingId).
 * Service tu gan tung truong (khong co from() vi so lieu tinh tu nhieu nguon).
 */
@Getter
@Setter
public class RoyaltyGradingResponse {

    private Integer gradingId;          // null khi moi xem truoc
    private Integer articleId;
    private String title;
    private LocalDateTime publishedAt;

    // Cac buoc cong thuc (BR-RY-01..03)
    private Integer scoringAttemptId;   // lan cham co diem dang dung (BF-02)
    private BigDecimal scoreUsed;       // diem da duyet, thang 0-100
    private Integer categoryId;
    private BigDecimal baseFactor;      // he so chuan the loai
    private BigDecimal suggestedFactor; // = baseFactor x diem / 100
    private BigDecimal saturationRate;  // % bao hoa su kien
    private RoyaltyNature nature;       // tinh chat Thu ky xac nhan
    private BigDecimal natureRate;      // % theo tinh chat
    private BigDecimal finalFactor;     // he so cuoi, toi thieu 1
    private BigDecimal unitPrice;       // don gia hieu luc tai ngay dang

    // Tien
    private BigDecimal articleAmount;   // tien phan bai viet = finalFactor x unitPrice, lam tron xuong
    private BigDecimal mediaAmount;     // tong tien anh/video duoc tinh
    private BigDecimal totalAmount;     // = articleAmount + mediaAmount

    // Ky nhuan but bai se vao (ky dang OPEN, BR-RY-05); null khi chua co ky OPEN
    private Integer periodId;
    private Short periodYear;
    private Byte periodMonth;

    private List<RoyaltyGradingAuthorResponse> authors;   // tac gia chinh dung dau
    private List<RoyaltyGradingMediaResponse> mediaItems;
}
