package com.swp391.kpi.repository;

import java.time.LocalDateTime;

/**
 * 1 dong ket qua cua cau SQL ArticleRepository.findPublishedRows().
 * Day la "interface projection" cua Spring Data: Spring tu tao object, moi getter
 * lay dung cot co ten bi danh (AS ...) giong ten getter. Khong phai entity, khong co bang.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  getId()                         cot id cua cau query
 *   23  getTitle()                      cot title cua cau query
 *   24  getDeclaredNature()             cot declaredNature cua cau query
 *   25  getPublishedAt()                cot publishedAt cua cau query
 *   27  getAuthorId()                   cot authorId cua cau query
 *   29  getSectionId()                  cot sectionId cua cau query
 *   30  getSectionName()                cot sectionName cua cau query
 *   32  getCategoryId()                 cot categoryId cua cau query
 *   33  getCategoryName()               cot categoryName cua cau query
 */
public interface ArticlePublishedRow {
    Integer getId();
    String getTitle();
    String getDeclaredNature();
    LocalDateTime getPublishedAt();

    Integer getAuthorId();

    Integer getSectionId();
    String getSectionName();

    Integer getCategoryId();
    String getCategoryName();
}
