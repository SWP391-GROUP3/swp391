package com.swp391.kpi.assessment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO tra ve thong tin bai viet SUBMITTED cho Truong ban phan cong (UC-18).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  field                           cac truong tra ve cho giao dien
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

