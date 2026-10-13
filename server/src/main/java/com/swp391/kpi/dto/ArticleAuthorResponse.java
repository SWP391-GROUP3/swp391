package com.swp391.kpi.dto;

import com.swp391.kpi.entity.ArticleAuthor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 1 tac gia trong man chi tiet bai (UC-02): ty le va trang thai xac nhan.
 */
@Getter
@Setter
public class ArticleAuthorResponse {

    private Integer userId;            // TODO(admin): tra them ho ten khi co User
    private boolean mainAuthor;
    private BigDecimal sharePercent;
    private String confirmStatus;
    private LocalDateTime confirmDeadline;
    private LocalDateTime respondedAt;
    private String rejectReason;

    public static ArticleAuthorResponse from(ArticleAuthor author, Integer mainAuthorId) {
        ArticleAuthorResponse dto = new ArticleAuthorResponse();
        dto.setUserId(author.getUserId());
        dto.setMainAuthor(author.getUserId().equals(mainAuthorId));
        dto.setSharePercent(author.getSharePercent());
        dto.setConfirmStatus(author.getConfirmStatus().name());
        dto.setConfirmDeadline(author.getConfirmDeadline());
        dto.setRespondedAt(author.getRespondedAt());
        dto.setRejectReason(author.getRejectReason());
        return dto;
    }
}
