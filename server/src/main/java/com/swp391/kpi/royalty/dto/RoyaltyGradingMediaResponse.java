package com.swp391.kpi.royalty.dto;

import com.swp391.kpi.article.dto.ArticleRoyaltyAttachmentResponse;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Ket qua cham 1 anh/video (UC-48, BR-RY-04).
 * payable = false thi kem ly do (nguon ngoai, tai su dung) va khong tao dong tien.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  field                           cac truong tra ve cho giao dien
 *   32  from()                          noRoyaltyReason = null nghia la anh duoc tinh tien
 */
@Getter
@Setter
public class RoyaltyGradingMediaResponse {

    private Integer attachmentId;
    private Integer authorId;           // tac gia anh, nguoi nhan tien (dong MEDIA)
    private String mediaKind;           // PHOTO / GRAPHIC / VIDEO
    private String caption;
    private String url;
    private Boolean payable;            // co duoc tinh tien khong
    private String noRoyaltyReason;     // ly do khong tinh tien, null khi payable = true
    private BigDecimal factor;          // he so co dinh cua loai, null khi khong tinh tien
    private BigDecimal amount;          // VND, 0 khi khong tinh tien

    // noRoyaltyReason = null nghia la anh duoc tinh tien
    public static RoyaltyGradingMediaResponse from(ArticleRoyaltyAttachmentResponse attachment, BigDecimal factor,
                                                   BigDecimal amount, String noRoyaltyReason) {
        RoyaltyGradingMediaResponse response = new RoyaltyGradingMediaResponse();
        response.setAttachmentId(attachment.getId());
        response.setAuthorId(attachment.getAuthorId());
        response.setMediaKind(attachment.getMediaKind());
        response.setCaption(attachment.getCaption());
        response.setUrl(attachment.getUrl());
        response.setPayable(noRoyaltyReason == null);
        response.setNoRoyaltyReason(noRoyaltyReason);
        response.setFactor(factor);
        response.setAmount(amount);
        return response;
    }
}
