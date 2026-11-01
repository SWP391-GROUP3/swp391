package com.swp391.kpi.repository;

import com.swp391.kpi.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Doc bang events (su kien, BF-04). findById() co san dung de lay exemptScore
 * khi cham nhuan but (GBR-09) va k_limit khi doi hang ra ty le bao hoa.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   23  findOpenOn()                    Su kien dang mo tai 1 ngay: is_active = 1 va ngay nam trong [start_date, end_date]
 */
public interface EventRepository extends JpaRepository<Event, Integer> {

    // Su kien dang mo tai 1 ngay: is_active = 1 va ngay nam trong [start_date, end_date].
    // BF-01 dung cho o chon su kien luc soan/nop bai (BR-IN-03). Su kien bat dau som len dau.
    @Query("SELECT e FROM Event e WHERE e.isActive = true AND e.startDate <= :date AND e.endDate >= :date ORDER BY e.startDate")
    List<Event> findOpenOn(@Param("date") LocalDate date);
}
