package com.swp391.kpi.article.dto;

import com.swp391.kpi.article.entity.ArticleStatusLog;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 1 dong lich su trang thai trong man chi tiet bai (UC-02).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   20  field                           cac truong tra ve cho giao dien
 *   28  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class ArticleStatusLogResponse {

    private Integer actorId;
    private String actorName;    // ho ten nguoi thao tac, lay qua UserReadService
    private String action;
    private String fromStatus;   // null o lan tao bai
    private String toStatus;
    private String note;
    private LocalDateTime createdAt;

    public static ArticleStatusLogResponse from(ArticleStatusLog log, String actorName) {
        ArticleStatusLogResponse dto = new ArticleStatusLogResponse();
        dto.setActorId(log.getActorId());
        dto.setActorName(actorName);
        dto.setAction(log.getAction());
        if (log.getFromStatus() != null) {
            dto.setFromStatus(log.getFromStatus().name());
        }
        dto.setToStatus(log.getToStatus().name());
        dto.setNote(log.getNote());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
