package com.swp391.kpi.article.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Noi dung file anh tra ve cho controller (Q7: GET /api/articles/{id}/attachments/{attachmentId}/file).
 * Controller gui thang cac byte nay ve trinh duyet, khong boc trong ApiResponse.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   17  field                           cac truong tra ve cho giao dien
 */
@Getter
@Setter
public class ArticleAttachmentFileResponse {

    private byte[] content;
    private String contentType;   // image/jpeg hoac image/png
}
