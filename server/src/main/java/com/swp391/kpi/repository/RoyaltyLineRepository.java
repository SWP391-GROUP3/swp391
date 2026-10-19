package com.swp391.kpi.repository;

import com.swp391.kpi.entity.RoyaltyLine;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Doc/ghi bang royalty_lines (dong tien cua tung nguoi trong ky).
 * UC-47/48 chi dung save() co san de ghi dong ARTICLE / MEDIA.
 */
public interface RoyaltyLineRepository extends JpaRepository<RoyaltyLine, Integer> {
}
