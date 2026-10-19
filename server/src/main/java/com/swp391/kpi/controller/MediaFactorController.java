package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.MediaFactorResponse;
import com.swp391.kpi.dto.MediaFactorUpdateRequest;
import com.swp391.kpi.service.MediaFactorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API he so anh/video cho man Media Factor (UC-56, Admin).
 *
 * MUC LUC (so dong)
 *   32  field                        - service duoc inject
 *   36  getAllMediaFactors()         - GET /api/media-factors: danh sach loai anh/video va he so
 *   43  updateFactor()               - PUT /api/media-factors/{id}: sua he so 1 loai
 */
@RestController
@RequestMapping("/api/media-factors")
@RequiredArgsConstructor
public class MediaFactorController {

    private final MediaFactorService mediaFactorService;

    // GET /api/media-factors: danh sach loai anh/video va he so
    @GetMapping
    public ResponseEntity<ApiResponse<List<MediaFactorResponse>>> getAllMediaFactors() {
        List<MediaFactorResponse> data = mediaFactorService.getAllMediaFactors();
        return ResponseEntity.ok(ApiResponse.success("Lấy hệ số ảnh/video thành công", data));
    }

    // PUT /api/media-factors/{id}: sua he so 1 loai
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MediaFactorResponse>> updateFactor(
            @PathVariable Integer id,
            @Valid @RequestBody MediaFactorUpdateRequest request) {
        MediaFactorResponse data = mediaFactorService.updateFactor(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hệ số thành công", data));
    }
}
