package com.swp391.kpi.admin.repository;

import com.swp391.kpi.admin.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Doc bang genres (the loai).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   17  findByIsActiveTrueOrderByNameAsc()  Cac the loai dang dung (is_active = 1), xep theo ten
 */
public interface GenreRepository extends JpaRepository<Genre, Integer> {

    // Cac the loai dang dung (is_active = 1), xep theo ten. Spring tu sinh cau SQL tu ten ham.
    List<Genre> findByIsActiveTrueOrderByNameAsc();
}
