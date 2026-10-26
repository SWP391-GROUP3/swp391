package com.swp391.kpi.dto;

import com.swp391.kpi.entity.ArticleNature;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Tao / sua ban nhap (UC-03) kem danh sach tac gia + ty le (UC-04).
 * Noi dung duoc de trong khi luu nhap, chi bat buoc luc nop (UC-05).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   26  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class ArticleSaveRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 300, message = "Tiêu đề tối đa {max} ký tự")
    private String title;

    private String content;

    @NotNull(message = "Vui lòng chọn thể loại")
    private Integer declaredCategoryId;

    // Bat buoc chon ngay khi tao nhap, khong dua vao DEFAULT 'ORIGINAL' cua DB
    // (nhom truong 24/10, B3: BF-04 tinh nhuan but thang theo tinh chat PV khai)
    @NotNull(message = "Vui lòng chọn tính chất bài")
    private ArticleNature declaredNature;

    // Gom ca tac gia chinh. Rong / khong gui = chi tac gia chinh 100%
    @Valid
    private List<ArticleAuthorShareRequest> authors;
}
