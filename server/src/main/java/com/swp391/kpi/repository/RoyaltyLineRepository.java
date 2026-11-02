package com.swp391.kpi.repository;

import com.swp391.kpi.entity.RoyaltyLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Doc/ghi bang royalty_lines (dong tien cua tung nguoi trong ky).
 * UC-47/48 chi dung save() co san de ghi dong ARTICLE / MEDIA.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  findByPeriodId()                Moi dong tien cua 1 ky (bai, anh, dieu chinh), de cong theo tung nguoi (UC-50)
 *   24  deleteByGradingId()             Cham lai trong ky OPEN (nhom chot 27/10/2026): xoa dong tien cua phien ban cu
 *   27  findByUserIdOrderByCreatedAtDesc() UC-57: moi dong tien cua 1 nguoi qua moi ky
 */
public interface RoyaltyLineRepository extends JpaRepository<RoyaltyLine, Integer> {

    // Moi dong tien cua 1 ky (bai, anh, dieu chinh), de cong theo tung nguoi (UC-50)
    List<RoyaltyLine> findByPeriodId(Integer periodId);

    // Cham lai trong ky OPEN (nhom chot 27/10/2026): xoa dong tien cua phien ban cu.
    // Chi duoc goi khi ky con OPEN; ky da khoa thi khong bao gio xoa (BR-RY-06).
    // Spring tu viet cau DELETE tu ten ham; service goi phai co @Transactional.
    void deleteByGradingId(Integer gradingId);

    // UC-57: moi dong tien cua 1 nguoi (bai, anh, dieu chinh) qua moi ky, moi nhat len dau
    List<RoyaltyLine> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
