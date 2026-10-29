package com.swp391.kpi.repository;

import com.swp391.kpi.entity.MediaFactor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Doc/ghi bang media_factors (he so anh/video, UC-56).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  findAllByOrderByMediaKindAsc()  Danh sach loai anh/video, xep theo ten loai
 *   22  findByMediaKind()               He so cua 1 loai anh/video (PHOTO / GRAPHIC / VIDEO), dung khi cham anh (UC-48)
 */
public interface MediaFactorRepository extends JpaRepository<MediaFactor, Integer> {

    // Danh sach loai anh/video, xep theo ten loai
    List<MediaFactor> findAllByOrderByMediaKindAsc();

    // He so cua 1 loai anh/video (PHOTO / GRAPHIC / VIDEO), dung khi cham anh (UC-48)
    Optional<MediaFactor> findByMediaKind(String mediaKind);
}
