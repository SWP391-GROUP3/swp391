package com.swp391.kpi.assessment.controller;

import com.swp391.kpi.assessment.dto.CriteriaSetVersionCreateRequest;
import com.swp391.kpi.assessment.dto.CriteriaSetVersionResponse;
import com.swp391.kpi.assessment.service.CriteriaSetService;
import com.swp391.kpi.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller cho Quản lý bộ tiêu chí chấm (Admin - Luồng BF-02).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   32  field                           service duoc inject
 *   38  getVersionsByCategory()         Lay danh sach cac phien ban bo tieu chi theo ma the loai (UC-1… [GET /api/criteria-sets/category/{categoryId}]
 *   48  getActiveVersionByCategory()    Lay phien ban bo tieu chi dang ACTIVE cua the loai (BR-… [GET /api/criteria-sets/category/{categoryId}/active]
 *   58  createDraftVersion()            Tao mot phien ban bo tieu chi moi o trang thai DRAFT (UC-15, UC-16, BR-SC-01) [POST /api/criteria-sets]
 *   71  activateVersion()               Kich hoat mot phien ban DRAFT thanh ACTIVE (UC-17, BR-SC-02) [PUT /api/criteria-sets/{versionId}/activate]
 */
@RestController
@RequestMapping("/api/criteria-sets")
@RequiredArgsConstructor
public class CriteriaSetController {

    private final CriteriaSetService criteriaSetService;

    /**
     * Lấy danh sách các phiên bản bộ tiêu chí theo mã thể loại (UC-14).
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<CriteriaSetVersionResponse>>> getVersionsByCategory(
            @PathVariable Integer categoryId) {
        List<CriteriaSetVersionResponse> data = criteriaSetService.getVersionsByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bộ tiêu chí thành công", data));
    }

    /**
     * Lấy phiên bản bộ tiêu chí đang ACTIVE của thể loại (BR-SC-03).
     */
    @GetMapping("/category/{categoryId}/active")
    public ResponseEntity<ApiResponse<CriteriaSetVersionResponse>> getActiveVersionByCategory(
            @PathVariable Integer categoryId) {
        CriteriaSetVersionResponse data = criteriaSetService.getActiveVersionByCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success("Lấy bộ tiêu chí ACTIVE thành công", data));
    }

    /**
     * Tạo một phiên bản bộ tiêu chí mới ở trạng thái DRAFT (UC-15, UC-16, BR-SC-01).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CriteriaSetVersionResponse>> createDraftVersion(
            @Valid @RequestBody CriteriaSetVersionCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        CriteriaSetVersionResponse data = criteriaSetService.createDraftVersion(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo phiên bản bộ tiêu chí thành công", data));
    }

    /**
     * Kích hoạt một phiên bản DRAFT thành ACTIVE (UC-17, BR-SC-02).
     */
    @PutMapping("/{versionId}/activate")
    public ResponseEntity<ApiResponse<CriteriaSetVersionResponse>> activateVersion(
            @PathVariable Integer versionId,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        CriteriaSetVersionResponse data = criteriaSetService.activateVersion(versionId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Kích hoạt phiên bản bộ tiêu chí thành công", data));
    }
}
