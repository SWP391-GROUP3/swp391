package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.AppealRuleConfigCreateRequest;
import com.swp391.kpi.dto.AppealRuleConfigResponse;
import com.swp391.kpi.service.AppealRuleConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appeal-rule-configs")
@RequiredArgsConstructor
public class AppealRuleConfigController {

    private final AppealRuleConfigService appealRuleConfigService;

    /**
     * Lay cau hinh dang ap dung hien tai.
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<AppealRuleConfigResponse>> getActiveConfig() {
        AppealRuleConfigResponse data = appealRuleConfigService.getActiveConfig();
        return ResponseEntity.ok(ApiResponse.success("Lấy cấu hình khiếu nại đang áp dụng thành công", data));
    }

    /**
     * Lay toan bo lich su cac phien ban cau hinh (Admin xem).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AppealRuleConfigResponse>>> getAllConfigs() {
        List<AppealRuleConfigResponse> data = appealRuleConfigService.getAllConfigs();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách cấu hình khiếu nại thành công", data));
    }

    /**
     * Tao phien ban cau hinh khiieu nai moi co hieu luc theo ngay (UC-44).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AppealRuleConfigResponse>> createConfig(
            @Valid @RequestBody AppealRuleConfigCreateRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "8") Integer currentUserId // TODO(auth): lay tu token
    ) {
        AppealRuleConfigResponse data = appealRuleConfigService.createConfig(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo cấu hình khiếu nại mới thành công", data));
    }
}
