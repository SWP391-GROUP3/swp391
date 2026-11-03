package com.swp391.kpi.kpi.repository;

import com.swp391.kpi.kpi.entity.KpiTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác dữ liệu với bảng kpi_targets.
 * Sử dụng các query method chuẩn của Spring Data JPA.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   22  findByTargetYearAndTargetMonth()  Lay danh sach chi tieu cua ca ban trong 1 ky (nam, thang) cho man hinh UC-59
 *   25  findByReporterIdAndTargetYearAndTargetMonth()  Lay chi tieu cu the cua 1 phong vien trong 1 ky
 *   28  existsByReporterIdAndTargetYearAndTargetMonth()  Kiem tra xem phong vien da co chi tieu trong thang nay chua (phuc vu BR-KPI-01)
 *   31  findByReporterIdOrderByTargetYearDescTargetMonthDesc()  Lay lich su chi tieu cua mot phong vien
 */
public interface KpiTargetRepository extends JpaRepository<KpiTarget, Integer> {

    // Lấy danh sách chỉ tiêu của cả ban trong 1 kỳ (năm, tháng) cho màn hình UC-59
    List<KpiTarget> findByTargetYearAndTargetMonth(Short targetYear, Byte targetMonth);

    // Lấy chỉ tiêu cụ thể của 1 phóng viên trong 1 kỳ
    Optional<KpiTarget> findByReporterIdAndTargetYearAndTargetMonth(Integer reporterId, Short targetYear, Byte targetMonth);

    // Kiểm tra xem phóng viên đã có chỉ tiêu trong tháng này chưa (phục vụ BR-KPI-01)
    boolean existsByReporterIdAndTargetYearAndTargetMonth(Integer reporterId, Short targetYear, Byte targetMonth);

    // Lấy lịch sử chỉ tiêu của một phóng viên
    List<KpiTarget> findByReporterIdOrderByTargetYearDescTargetMonthDesc(Integer reporterId);
}
