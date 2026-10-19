package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.UserPasswordChangeRequest;
import com.swp391.kpi.dto.UserProfileResponse;
import com.swp391.kpi.dto.UserProfileUpdateRequest;
import com.swp391.kpi.service.UserService;
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
 * Ho so cua nguoi dang dang nhap. Duong dan "/me" = "chinh toi":
 * id lay tu token, KHONG nhan id tu URL -> khong ai sua duoc ho so nguoi khac.
 * Moi vai tro deu dung duoc (SecurityConfig: anyRequest().authenticated()).
 *
 * MUC LUC (so dong)
 *   35  field                        - service duoc inject
 *   39  getMyProfile()               - GET /api/users/me
 *   47  updateMyProfile()            - PUT /api/users/me body: { "fullName": "...", "email": "..." }
 *   57  changeMyPassword()           - PUT /api/users/me/password body: { "currentPassword", "newPassword", "c…
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // GET /api/users/me
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        UserProfileResponse data = userService.getMyProfile(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ thành công", data));
    }

    // PUT /api/users/me  body: { "fullName": "...", "email": "..." }
    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(
            @Valid @RequestBody UserProfileUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        UserProfileResponse data = userService.updateMyProfile(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ thành công", data));
    }

    // PUT /api/users/me/password  body: { "currentPassword", "newPassword", "confirmPassword" }
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changeMyPassword(
            @Valid @RequestBody UserPasswordChangeRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        userService.changeMyPassword(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }
}
