package com.swp391.kpi.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO đại diện cho định mức chuẩn theo ngạch phóng viên (UC-58).
 * Phục vụ cấu hình và nút "Áp dụng định mức chuẩn" trong UC-59.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KpiBenchmarkConfigDto {

    // PROBATIONARY (Tập sự), JUNIOR (Sơ cấp), SENIOR (Kỳ cựu)
    private String seniorityLevel;

    // Tên hiển thị tiếng Việt
    private String levelName;

    // Số bài xuất bản tối thiểu mặc định
    private Integer defaultMinPublished;

    // Điểm chất lượng trung bình sàn mặc định
    private BigDecimal defaultMinAvgScore;
}
