package com.swp391.kpi.article.dto;

import com.swp391.kpi.article.entity.ArticleAttachment;
import com.swp391.kpi.article.entity.ArticleMediaType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 1 anh/video cua bai kem ket qua kiem trung (UC-03, UC-07, UC-02). Anh xem qua API
 * GET /api/articles/{id}/attachments/{attachmentId}/file; video thi videoUrl la link.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   27  field                           cac truong tra ve cho giao dien
 *   43  from()                          doi entity sang DTO (map tay, AGENTS 3.4)
 */
@Getter
@Setter
public class ArticleAttachmentResponse {

    // Ly do khong tinh nhuan but (B22, UC-05 A3)
    public static final String NO_ROYALTY_EXTERNAL = "EXTERNAL";   // anh lay tu nguon ngoai
    public static final String NO_ROYALTY_REUSED = "REUSED";       // trung anh da tung duoc tra nhuan but

    private Integer id;
    private String mediaType;            // IMAGE / VIDEO
    private String mediaKind;            // PHOTO, GRAPHIC, VIDEO...
    private Integer authorId;
    private String authorName;           // ho ten, lay qua UserReadService
    private String externalSource;
    private String videoUrl;             // chi co voi video
    private String caption;
    private boolean submitted;           // da gan vao 1 ban nop (version_id khac NULL)
    private String hashResult;           // null = chua kiem (chua nop) | NEW | DUPLICATE | SIMILAR
    private Integer distanceBits;
    private Integer matchedAttachmentId;
    private Integer matchedArticleId;    // bai chua anh bi trung, de hien "trung anh cua bai ..."
    private String noRoyaltyReason;      // null = duoc tinh tien | EXTERNAL | REUSED
    private LocalDateTime createdAt;

    public static ArticleAttachmentResponse from(ArticleAttachment attachment, String authorName) {
        ArticleAttachmentResponse dto = new ArticleAttachmentResponse();
        dto.setId(attachment.getId());
        dto.setMediaType(attachment.getMediaType().name());
        dto.setMediaKind(attachment.getMediaKind());
        dto.setAuthorId(attachment.getAuthorId());
        dto.setAuthorName(authorName);
        dto.setExternalSource(attachment.getExternalSource());
        if (attachment.getMediaType() == ArticleMediaType.VIDEO) {
            dto.setVideoUrl(attachment.getUrl());
        }
        dto.setCaption(attachment.getCaption());
        dto.setSubmitted(attachment.getVersionId() != null);
        dto.setHashResult(attachment.getHashResult() == null ? null : attachment.getHashResult().name());
        dto.setDistanceBits(attachment.getDistanceBits() == null ? null : attachment.getDistanceBits().intValue());
        dto.setMatchedAttachmentId(attachment.getMatchedAttachmentId());
        dto.setCreatedAt(attachment.getCreatedAt());
        return dto;
    }
}
