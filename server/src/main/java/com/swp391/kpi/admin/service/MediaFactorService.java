package com.swp391.kpi.admin.service;

import com.swp391.kpi.admin.dto.MediaFactorResponse;
import com.swp391.kpi.admin.dto.MediaFactorUpdateRequest;
import com.swp391.kpi.admin.entity.MediaFactor;
import com.swp391.kpi.admin.repository.MediaFactorRepository;
import com.swp391.kpi.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Quan ly he so anh/video (UC-56, bang media_factors).
 * Sua he so chi anh huong cac lan cham SAU do: phieu cham cu da chep he so
 * vao royalty_media_items.factor nen khong doi theo.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   31  field                           repository / service duoc inject
 *   35  getAllMediaFactors()            Danh sach loai anh/video va he so
 *   45  updateFactor()                  Admin sua he so cua 1 loai anh/video
 *   57  getFactor()                     M-D2 (cho BF-04): he so cua 1 loai anh/video (media_factors.media_kind: PHOTO / GRAPHIC / VIDEO)
 */
@Service
@RequiredArgsConstructor
public class MediaFactorService {

    private final MediaFactorRepository mediaFactorRepository;

    // Danh sach loai anh/video va he so
    @Transactional(readOnly = true)
    public List<MediaFactorResponse> getAllMediaFactors() {
        List<MediaFactorResponse> result = new ArrayList<>();
        for (MediaFactor mediaFactor : mediaFactorRepository.findAllByOrderByMediaKindAsc()) {
            result.add(MediaFactorResponse.from(mediaFactor));
        }
        return result;
    }

    // Admin sua he so cua 1 loai anh/video
    @Transactional
    public MediaFactorResponse updateFactor(Integer id, MediaFactorUpdateRequest request) {
        MediaFactor mediaFactor = mediaFactorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy loại ảnh/video có ID: " + id));

        mediaFactor.setFactor(request.getFactor());

        MediaFactor saved = mediaFactorRepository.save(mediaFactor);
        return MediaFactorResponse.from(saved);
    }

    // M-D2 (cho BF-04): he so cua 1 loai anh/video (media_factors.media_kind: PHOTO / GRAPHIC / VIDEO)
    @Transactional(readOnly = true)
    public BigDecimal getFactor(String mediaKind) {
        MediaFactor mediaFactor = mediaFactorRepository.findByMediaKind(mediaKind)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy hệ số cho loại: " + mediaKind));
        return mediaFactor.getFactor();
    }
}
