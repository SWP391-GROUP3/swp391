package com.swp391.kpi.repository;

import com.swp391.kpi.entity.MediaFactor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Doc/ghi bang media_factors (he so anh/video, UC-56).
 */
public interface MediaFactorRepository extends JpaRepository<MediaFactor, Integer> {

    // Danh sach loai anh/video, xep theo ten loai
    List<MediaFactor> findAllByOrderByMediaKindAsc();
}
