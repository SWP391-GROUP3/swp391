package com.swp391.kpi.repository;

import com.swp391.kpi.entity.ArticleAttachment;
import com.swp391.kpi.entity.ArticleMediaType;
import com.swp391.kpi.entity.ArticleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository truy van bang cua entity ArticleAttachment (Spring Data JPA). Chi service cung luong goi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  findByVersionIdOrderByIdAsc()   Anh/video da gan vao 1 ban (article_versions.id), anh them truoc xep truoc
 *   27  findByArticleIdOrderByIdAsc()   Anh/video hien tai cua 1 bai (quyet dinh 03/11: moi dong cua bai deu la anh hien tai), them truoc xep truoc
 *   32  findCompareCandidates()         B21: anh/video cung dinh dang cua cac bai KHAC da nop (trang thai khac DRAFT) de kiem trung (UC-07)
 *   37  existsByMatchedAttachmentId()   Co anh/video nao dang tro toi dong nay (matched_attachment_id) khong -> co thi khong xoa duoc
 */
public interface ArticleAttachmentRepository extends JpaRepository<ArticleAttachment, Integer> {

    // Anh/video da gan vao 1 ban (article_versions.id), anh them truoc xep truoc
    List<ArticleAttachment> findByVersionIdOrderByIdAsc(Integer versionId);

    // Anh/video hien tai cua 1 bai (quyet dinh 03/11: moi dong cua bai deu la anh hien tai), them truoc xep truoc
    List<ArticleAttachment> findByArticleIdOrderByIdAsc(Integer articleId);

    // B21: anh/video cung dinh dang cua cac bai KHAC da nop (trang thai khac DRAFT) de kiem trung (UC-07)
    @Query("SELECT m FROM ArticleAttachment m WHERE m.articleId <> :articleId AND m.mediaType = :mediaType"
            + " AND m.articleId IN (SELECT a.id FROM Article a WHERE a.status <> :draftStatus)")
    List<ArticleAttachment> findCompareCandidates(@Param("articleId") Integer articleId,
                                                  @Param("mediaType") ArticleMediaType mediaType,
                                                  @Param("draftStatus") ArticleStatus draftStatus);

    // Co anh/video nao dang tro toi dong nay (matched_attachment_id) khong -> co thi khong xoa duoc
    boolean existsByMatchedAttachmentId(Integer attachmentId);
}
