package com.swp391.kpi.dto;

import java.time.LocalDateTime;

/**
 * DTO tra ve cho giao dien. Dung o: AppealReviewController, CrossReviewController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   11  record                          cac truong khai bao trong ngoac cua record
 */
public record AppealReviewTaskResponse(Integer id, String title, LocalDateTime submittedAt) {}
