package com.swp391.kpi.repository;

import com.swp391.kpi.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Doc bang genres (the loai). Hien chi dung findById() co san cua JpaRepository.
 */
public interface GenreRepository extends JpaRepository<Genre, Integer> {
}
