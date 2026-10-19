package com.swp391.kpi.repository;

import com.swp391.kpi.entity.RoyaltyLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Doc/ghi bang royalty_lines (dong tien cua tung nguoi trong ky).
 * UC-47/48 chi dung save() co san de ghi dong ARTICLE / MEDIA.
 */
public interface RoyaltyLineRepository extends JpaRepository<RoyaltyLine, Integer> {

    // Moi dong tien cua 1 ky (bai, anh, dieu chinh), de cong theo tung nguoi (UC-50)
    List<RoyaltyLine> findByPeriodId(Integer periodId);
}
