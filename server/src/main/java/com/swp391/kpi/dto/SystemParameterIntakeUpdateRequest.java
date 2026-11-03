package com.swp391.kpi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Du lieu Admin gui len khi sua 6 tham so nhap bai (UC-13).
 * Kiem tra tung o o day; "nguong canh bao < nguong chan" can ca 2 gia tri nen kiem trong service.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   24  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class SystemParameterIntakeUpdateRequest {

    // GBR-01: 1 bai toi da 5 tac gia
    @NotNull(message = "Số tác giả tối đa không được để trống")
    @Min(value = 1, message = "Số tác giả tối đa phải từ 1 trở lên")
    @Max(value = 5, message = "Số tác giả tối đa không được quá 5 (GBR-01)")
    private Integer maxAuthorsPerArticle;

    @NotNull(message = "Số ngày xác nhận đồng tác giả không được để trống")
    @Min(value = 1, message = "Số ngày xác nhận đồng tác giả phải từ 1 trở lên")
    private Integer coAuthorConfirmDays;

    @NotNull(message = "Ngưỡng cảnh báo trùng lặp không được để trống")
    @Min(value = 1, message = "Ngưỡng cảnh báo trùng lặp phải lớn hơn 0%")
    @Max(value = 100, message = "Ngưỡng cảnh báo trùng lặp không được quá 100%")
    private Integer dupWarnPercent;

    @NotNull(message = "Ngưỡng chặn trùng lặp không được để trống")
    @Min(value = 1, message = "Ngưỡng chặn trùng lặp phải lớn hơn 0%")
    @Max(value = 100, message = "Ngưỡng chặn trùng lặp không được quá 100%")
    private Integer dupBlockPercent;

    // dHash 64 bit nen khoang cach Hamming chi tu 0 den 64
    @NotNull(message = "Ngưỡng dHash không được để trống")
    @Min(value = 0, message = "Ngưỡng dHash phải từ 0 trở lên")
    @Max(value = 64, message = "Ngưỡng dHash không được quá 64 bit")
    private Integer dhashMaxBits;

    // BR-IN-04: do dai 1 manh khi kiem trung noi dung. Qua ngan thi cum tu thong dung cung bi tinh trung,
    // qua dai thi sua 1 chu la lot -> nhom chot 3..10 am tiet (mac dinh 5)
    @NotNull(message = "Độ dài mảnh so trùng không được để trống")
    @Min(value = 3, message = "Độ dài mảnh so trùng phải từ 3 âm tiết trở lên")
    @Max(value = 10, message = "Độ dài mảnh so trùng không được quá 10 âm tiết")
    private Integer shingleSize;
}
