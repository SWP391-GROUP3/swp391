package com.swp391.kpi.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Body (khong bat buoc) cua nop / nop lai bai (UC-05, UC-10): giai trinh khi noi dung trung
 * tu DUP_WARN_PERCENT tro len (UC-08, B6).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class ArticleSubmitRequest {

    @Size(max = 1000, message = "Giải trình tối đa 1000 ký tự")   // duplication_checks.explanation NVARCHAR(1000)
    private String explanation;
}
