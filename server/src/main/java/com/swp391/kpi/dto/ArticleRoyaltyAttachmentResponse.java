package com.swp391.kpi.dto;

import com.swp391.kpi.entity.ArticleAttachment;
import lombok.Getter;
import lombok.Setter;

/**
 * 1 anh/video cua bai, dung de BF-04 cham anh/video (UC-48, BR-RY-04).
 * BF-04 dua vao externalSource, hashResult, matchedAttachmentId de biet anh nao
 * KHONG duoc tinh tien (nguon ngoai, hoac trung anh da tung tra nhuan but - BR-IN-05).
 *
 * MUC LUC (so dong)
 *   20  cac truong: id, authorId, mediaType, mediaKind, externalSource, hashResult, ...
 *   30  from(attachment)
 */
@Getter
@Setter
public class ArticleRoyaltyAttachmentResponse {

    private Integer id;
    private Integer authorId;           // nguoi nhan nhuan but phan anh nay
    private String mediaType;           // IMAGE / VIDEO
    private String mediaKind;           // loai tac pham, tra bang media_factors
    private String externalSource;      // khac null = nguon ngoai, khong tinh tien
    private String hashResult;          // null (chua kiem) / NEW / DUPLICATE / SIMILAR
    private Integer matchedAttachmentId;// anh bi trung (khi DUPLICATE / SIMILAR)
    private String caption;
    private String url;

    public static ArticleRoyaltyAttachmentResponse from(ArticleAttachment attachment) {
        ArticleRoyaltyAttachmentResponse dto = new ArticleRoyaltyAttachmentResponse();
        dto.setId(attachment.getId());
        dto.setAuthorId(attachment.getAuthorId());
        dto.setMediaType(attachment.getMediaType().name());
        dto.setMediaKind(attachment.getMediaKind());
        dto.setExternalSource(attachment.getExternalSource());
        // hash_result co the NULL (chua kiem) nen kiem truoc khi goi name()
        if (attachment.getHashResult() != null) {
            dto.setHashResult(attachment.getHashResult().name());
        }
        dto.setMatchedAttachmentId(attachment.getMatchedAttachmentId());
        dto.setCaption(attachment.getCaption());
        dto.setUrl(attachment.getUrl());
        return dto;
    }
}
