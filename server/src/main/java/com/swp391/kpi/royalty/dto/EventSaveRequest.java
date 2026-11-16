package com.swp391.kpi.royalty.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Du lieu Thu ky toa soan gui len khi TAO hoac SUA 1 su kien bao hoa (bang events, GBR-09).
 * O day chi kiem DINH DANG tung o (rong, qua dai, ngoai khoang). Luat lien quan nhieu o
 * hoac phai hoi DB (ngay bat dau <= ngay ket thuc, trung ten...) kiem trong EventService.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   32  hang so                         gioi han do dai / gia tri (khong viet so ma trong annotation)
 *   36  field                         cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class EventSaveRequest {

    // Gioi han cua tung o. NAME_MAX_LENGTH = cot events.name NVARCHAR(200);
    // 2 so con lai schema.sql chi yeu cau > 0, dat tran de chan so nhap nham.
    public static final int NAME_MAX_LENGTH = 200;
    public static final int MAX_WINDOW_DAYS = 365;          // cua so dem hang toi da 1 nam
    public static final int MAX_ARTICLES_PER_LEVEL = 1000;  // so bai moi bac bao hoa

    @NotBlank(message = "Tên sự kiện không được để trống")
    @Size(max = NAME_MAX_LENGTH, message = "Tên sự kiện tối đa {max} ký tự")
    private String name;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    // = events.window_days: chi dem cac bai nop trong N ngay gan nhat de xep hang bao hoa
    @NotNull(message = "Cửa sổ tính bão hoà không được để trống")
    @Min(value = 1, message = "Cửa sổ tính bão hoà phải từ {value} ngày trở lên")
    @Max(value = MAX_WINDOW_DAYS, message = "Cửa sổ tính bão hoà tối đa {value} ngày")
    private Integer windowDays;

    // = events.k_limit. Dat ten giong EventResponse.articlesPerLevel vi Jackson doc sai ten "kLimit"
    @NotNull(message = "Số bài mỗi bậc bão hoà không được để trống")
    @Min(value = 1, message = "Số bài mỗi bậc bão hoà phải từ {value} trở lên")
    @Max(value = MAX_ARTICLES_PER_LEVEL, message = "Số bài mỗi bậc bão hoà tối đa {value}")
    private Integer articlesPerLevel;

    // = events.exempt_score DECIMAL(5,2): bai co diem >= muc nay thi khong bi giam (GBR-09)
    @NotNull(message = "Điểm miễn giảm không được để trống")
    @DecimalMin(value = "0", message = "Điểm miễn giảm phải từ 0 đến 100")
    @DecimalMax(value = "100", message = "Điểm miễn giảm phải từ 0 đến 100")
    @Digits(integer = 3, fraction = 2, message = "Điểm miễn giảm chỉ được tối đa 2 chữ số thập phân")
    private BigDecimal exemptScore;
}
