package com.swp391.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO ket qua sau khi Truong ban duyet xuat ban bai viet thanh cong (UC-33).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublishingResponse {

    private Integer articleId;
    private String status;
    private LocalDateTime publishedAt;
    private Integer approvedBy;
    private String message;
}
