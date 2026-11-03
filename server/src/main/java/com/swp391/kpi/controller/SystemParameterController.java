package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.SystemParameterIntakeResponse;
import com.swp391.kpi.dto.SystemParameterIntakeUpdateRequest;
import com.swp391.kpi.service.SystemParameterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API tham so nhap bai cho man Configure Intake Parameters (UC-13, chi Admin - xem SecurityConfig).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   31  field                           service duoc inject
 *   35  getIntakeParameters()           lay intake parameters [GET /api/system-parameters/intake]
 *   42  updateIntakeParameters()        cap nhat intake parameters [PUT /api/system-parameters/intake]
 */
@RestController
@RequestMapping("/api/system-parameters")
@RequiredArgsConstructor
public class SystemParameterController {

    private final SystemParameterService systemParameterService;

    // GET /api/system-parameters/intake: 6 tham so nhap bai hien tai
    @GetMapping("/intake")
    public ResponseEntity<ApiResponse<SystemParameterIntakeResponse>> getIntakeParameters() {
        SystemParameterIntakeResponse data = systemParameterService.getIntakeParameters();
        return ResponseEntity.ok(ApiResponse.success("Lấy tham số nhập bài thành công", data));
    }

    // PUT /api/system-parameters/intake: Admin sua 6 tham so nhap bai
    @PutMapping("/intake")
    public ResponseEntity<ApiResponse<SystemParameterIntakeResponse>> updateIntakeParameters(
            @Valid @RequestBody SystemParameterIntakeUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());   // id nguoi dang nhap, lay tu token
        SystemParameterIntakeResponse data = systemParameterService.updateIntakeParameters(request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật tham số nhập bài thành công", data));
    }
}
