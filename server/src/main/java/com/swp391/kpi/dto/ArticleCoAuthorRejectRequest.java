package com.swp391.kpi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Dong tac gia tu choi (UC-12 A1): bat buoc ly do
 * (CHECK ck_article_authors_reject, cot reject_reason NVARCHAR(500)).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   21  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class ArticleCoAuthorRejectRequest {

    @NotBlank(message = "Vui lòng nhập lý do từ chối")
    @Size(max = 500, message = "Lý do tối đa {max} ký tự")
    private String reason;
}
