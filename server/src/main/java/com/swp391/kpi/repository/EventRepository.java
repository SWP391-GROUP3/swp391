package com.swp391.kpi.repository;

import com.swp391.kpi.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Doc bang events (su kien, BF-04). Hien chi dung findById() co san
 * de lay exemptScore khi cham nhuan but (GBR-09).
 */
public interface EventRepository extends JpaRepository<Event, Integer> {
}
