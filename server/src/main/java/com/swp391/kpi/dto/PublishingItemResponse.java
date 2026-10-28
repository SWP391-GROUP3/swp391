package com.swp391.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO dai dien cho 1 dong trong Hang cho duyet xuat ban cua Truong ban (UC-33).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   23  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublishingItemResponse {

    private Integer articleId;
    private String title;
    private String authorName;
    private Integer categoryId;
    private String categoryName;
    private BigDecimal totalScore;
    private String reviewerName;
    private LocalDateTime scoreApprovedAt;
    private LocalDateTime appealDeadline;
    private Boolean isAppealWindowOpen;
    private Integer daysRemaining;
    private LocalDateTime updatedAt;
}
