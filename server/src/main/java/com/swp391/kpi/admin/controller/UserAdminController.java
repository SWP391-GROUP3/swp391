package com.swp391.kpi.admin.controller;

import com.swp391.kpi.admin.dto.UserAccessResponse;
import com.swp391.kpi.admin.dto.UserCreateRequest;
import com.swp391.kpi.admin.dto.UserProfileResponse;
import com.swp391.kpi.admin.service.UserAdminService;
import com.swp391.kpi.admin.service.UserReadService;
import com.swp391.kpi.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin quan ly tai khoan (man "Tài khoản", M-A7): xem, tao moi, khoa, mo khoa.
 * Chan vai tro 2 lop: SecurityConfig (hasRole ADMIN) + service (requireActive).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   41  field                           service duoc inject
 *   46  getUsers()                      lay users [GET /api/users]
 *   54  createUser()                    tao tai khoan [POST /api/users]
 *   65  lockUser()                      khoa tai khoan [PUT /api/users/{id}/lock]
 *   73  unlockUser()                    mo khoa tai khoan [PUT /api/users/{id}/unlock]
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserReadService userReadService;
    private final UserAdminService userAdminService;

    // GET /api/users: moi tai khoan (ho ten, vai tro, ban, trang thai)
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserAccessResponse>>> getUsers(@AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<UserAccessResponse> data = userReadService.getUsersForAdmin(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tài khoản thành công", data));
    }

    // POST /api/users  body: { username, email, password, fullName, roleCode, sectionId }
    @PostMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        UserProfileResponse data = userAdminService.createUser(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo tài khoản thành công", data));
    }

    // PUT /api/users/{id}/lock
    @PutMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<Void>> lockUser(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        userAdminService.lockUser(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã khoá tài khoản", null));
    }

    // PUT /api/users/{id}/unlock
    @PutMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockUser(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        userAdminService.unlockUser(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success("Đã mở khoá tài khoản", null));
    }
}
