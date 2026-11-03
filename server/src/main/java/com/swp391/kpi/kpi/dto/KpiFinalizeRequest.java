package com.swp391.kpi.kpi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * DTO yeu cau chot ky KPI thang va luu snapshot bat bien (UC-66, BR-KPI-06).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   18  field       nam, thang va danh sach nhan xet danh gia cho tung phong vien
 */
@Getter
@Setter
public class KpiFinalizeRequest {

    @NotNull(message = "Năm không được để trống")
    @Min(value = 2020, message = "Năm không hợp lệ")
    @Max(value = 2100, message = "Năm không hợp lệ")
    private Short year;

    @NotNull(message = "Tháng không được để trống")
    @Min(value = 1, message = "Tháng phải từ 1 đến 12")
    @Max(value = 12, message = "Tháng phải từ 1 đến 12")
    private Byte month;

    // Ban do ghi chu / nhan xet cho tung phong vien: reporterId -> notes (tuy chon)
    private Map<Integer, String> reporterNotes;
}
