package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ArticleAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MUC LUC (so dong)
 *   15  findByVersionIdOrderByIdAsc - anh/video gan voi 1 ban nop
 */
public interface ArticleAttachmentRepository extends JpaRepository<ArticleAttachment, Integer> {

    // Anh/video da gan vao 1 ban (article_versions.id), anh them truoc xep truoc
    List<ArticleAttachment> findByVersionIdOrderByIdAsc(Integer versionId);
}
