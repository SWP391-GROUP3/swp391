package com.swp391.kpi.admin.dto;

/**
 * DTO tra ve cho giao dien. Dung o: ArticleConfirmationService, ArticleEditorService,
 * ArticlePublishedService.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   10  record                          cac truong khai bao trong ngoac cua record
 */
public record UserAccessResponse(Integer id, String fullName, String role, Integer sectionId,
                                 String sectionName, String status) {}
