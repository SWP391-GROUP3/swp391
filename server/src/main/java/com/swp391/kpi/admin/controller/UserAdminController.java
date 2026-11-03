package com.swp391.kpi.admin.controller;

import com.swp391.kpi.admin.dto.UserAccessResponse;
import com.swp391.kpi.admin.service.UserReadService;
import com.swp391.kpi.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Admin xem danh sach tai khoan (man "Tài khoản", M-A7). Chi xem, them/sua tai khoan lam sau.
 * Chan vai tro o service (UserReadService.requireActive), luat SecurityConfig nho Quang them sau.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   29  field                           service duoc inject
 *   33  getUsers()                      lay users [GET /api/users]
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserReadService userReadService;

    // GET /api/users: moi tai khoan (ho ten, vai tro, ban, trang thai)
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserAccessResponse>>> getUsers(@AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<UserAccessResponse> data = userReadService.getUsersForAdmin(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tài khoản thành công", data));
    }
}
