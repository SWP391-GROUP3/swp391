package com.swp391.kpi.appeal.dto;

/**
 * DTO tra ve cho giao dien. Dung o: AppealManagementController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *    9  record                          cac truong khai bao trong ngoac cua record
 */
public record AppealReviewerCandidateResponse(Integer reviewerId, String reviewerName,
        Integer sectionId, String sectionName, boolean crossDepartment) {}
