package com.swp391.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * So bai cua nguoi dung o 1 trang thai (trang chu Phong vien).
 * Dang du lieu khop client/src/api/reporterHomeApi.js: { status, count }.
 *
 * MUC LUC (so dong)
 *   19  field                        - cac truong tra ve cho giao dien
 */
@Getter
@Setter
@AllArgsConstructor
public class ArticleStatusCountResponse {

    private String status;
    private long count;
}
