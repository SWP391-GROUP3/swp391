package com.swp391.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO tra ve thong tin bai viet SUBMITTED cho Truong ban phan cong (UC-18).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendingAssignmentResponse {
    private Integer id;
    private String title;
    private String author;
    private String category;
    private Integer categoryId;
    private LocalDateTime submittedAt;
    private Integer sectionId;
}

