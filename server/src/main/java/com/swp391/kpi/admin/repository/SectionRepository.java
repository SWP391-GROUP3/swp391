package com.swp391.kpi.admin.repository;

import com.swp391.kpi.admin.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Doc bang sections (ban).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   17  findByIsActiveTrueOrderByNameAsc()  Cac ban dang hoat dong, xep theo ten (danh sach chon tren form)
 */
public interface SectionRepository extends JpaRepository<Section, Integer> {

    // Cac ban dang hoat dong (is_active = 1), xep theo ten. Spring tu sinh cau SQL tu ten ham.
    List<Section> findByIsActiveTrueOrderByNameAsc();
}
