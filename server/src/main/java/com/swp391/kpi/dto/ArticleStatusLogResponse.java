package com.swp391.kpi.dto;

import com.swp391.kpi.entity.ArticleStatusLog;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 1 dong lich su trang thai trong man chi tiet bai (UC-02).
 */
@Getter
@Setter
public class ArticleStatusLogResponse {

    private Integer actorId;     // TODO(admin): tra them ho ten khi co User
    private String action;
    private String fromStatus;   // null o lan tao bai
    private String toStatus;
    private String note;
    private LocalDateTime createdAt;

    public static ArticleStatusLogResponse from(ArticleStatusLog log) {
        ArticleStatusLogResponse dto = new ArticleStatusLogResponse();
        dto.setActorId(log.getActorId());
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
