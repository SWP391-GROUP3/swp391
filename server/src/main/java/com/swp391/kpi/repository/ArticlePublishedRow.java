package com.swp391.kpi.repository;

import java.time.LocalDateTime;

/**
 * 1 dong ket qua cua cau SQL ArticleRepository.findPublishedRows().
 * Day la "interface projection" cua Spring Data: Spring tu tao object, moi getter
 * lay dung cot co ten bi danh (AS ...) giong ten getter. Khong phai entity, khong co bang.
 *
 * MUC LUC (so dong)
 *   17  bai:      getId, getTitle, getDeclaredNature, getPublishedAt
 *   22  tac gia:  getAuthorId
 *   24  ban:      getSectionId, getSectionName
 *   27  the loai: getCategoryId, getCategoryName
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
