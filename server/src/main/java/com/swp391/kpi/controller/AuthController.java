package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.LoginRequest;
import com.swp391.kpi.dto.LoginResponse;
import com.swp391.kpi.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API dang nhap (UC-69). Duong dan nay duoc mo cho nguoi chua dang nhap (SecurityConfig).
 *
 * MUC LUC (so dong)
 *   27  field                        - service duoc inject
 *   31  login()                      - POST /api/auth/login body: { "login": "an.nv@toasoan.vn", "password": "…
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/login  body: { "login": "an.nv@toasoan.vn", "password": "123456" }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
    }
}
