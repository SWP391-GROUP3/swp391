package com.swp391.kpi.dto;

/**
 * DTO tra ve cho giao dien. Dung o: AppealManagementService, ArticleReadService.
 *
 * MUC LUC (so dong - ten - tac dung)
 *    9  record                          cac truong khai bao trong ngoac cua record
 */
public record ArticleReadResponse(Integer id, Integer authorId, Integer sectionId, String title, String status) {}
