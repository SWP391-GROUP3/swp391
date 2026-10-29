package com.swp391.kpi.dto;

/**
 * DTO tra ve cho giao dien. Dung o: ReviewerProfileController.
 *
 * MUC LUC (so dong - ten - tac dung)
 *    9  record                          cac truong khai bao trong ngoac cua record
 */
public record ReviewerDirectoryResponse(Integer editorId, String editorName, Integer sectionId,
        String sectionName, String status) {}
