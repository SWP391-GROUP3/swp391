package com.swp391.kpi.appeal.controller;

import com.swp391.kpi.appeal.dto.AppealRuleConfigCreateRequest;
import com.swp391.kpi.appeal.dto.AppealRuleConfigResponse;
import com.swp391.kpi.appeal.service.AppealRuleConfigService;
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
 * API cau hinh luat khieu nai (UC-44): xem cau hinh dang ap dung, lich su, them cau hinh moi.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   31  field                           service duoc inject
 *   37  getActiveConfig()               Lay cau hinh dang ap dung hien tai [GET /api/appeal-rule-configs/active]
 *   48  getAllConfigs()                 Lay toan bo lich su cac phien ban cau hinh (Admin xem) [GET /api/appeal-rule-configs]
 *   59  createConfig()                  Tao phien ban cau hinh khiieu nai moi co hieu luc theo ngay (UC-44) [POST /api/appeal-rule-configs]
 */
@RestController
@RequestMapping("/api/appeal-rule-configs")
@RequiredArgsConstructor
public class AppealRuleConfigController {

    private final AppealRuleConfigService appealRuleConfigService;

    /**
     * Lay cau hinh dang ap dung hien tai.
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<AppealRuleConfigResponse>> getActiveConfig(
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        AppealRuleConfigResponse data = appealRuleConfigService.getActiveConfig(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy cấu hình khiếu nại đang áp dụng thành công", data));
    }

    /**
     * Lay toan bo lich su cac phien ban cau hinh (Admin xem).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AppealRuleConfigResponse>>> getAllConfigs(
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<AppealRuleConfigResponse> data = appealRuleConfigService.getAllConfigs(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cấu hình khiếu nại thành công", data));
    }

    /**
     * Tao phien ban cau hinh khiieu nai moi co hieu luc theo ngay (UC-44).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AppealRuleConfigResponse>> createConfig(
            @Valid @RequestBody AppealRuleConfigCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());   // id nguoi dang nhap, lay tu token
        AppealRuleConfigResponse data = appealRuleConfigService.createConfig(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo cấu hình khiếu nại mới thành công", data));
    }
}
