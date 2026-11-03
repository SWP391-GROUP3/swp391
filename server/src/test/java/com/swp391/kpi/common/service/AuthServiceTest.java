package com.swp391.kpi.common.service;

import com.swp391.kpi.admin.entity.Role;
import com.swp391.kpi.admin.entity.RoleCode;
import com.swp391.kpi.admin.entity.User;
import com.swp391.kpi.admin.entity.UserStatus;
import com.swp391.kpi.admin.repository.UserRepository;
import com.swp391.kpi.common.dto.LoginRequest;
import com.swp391.kpi.common.dto.LoginResponse;
import com.swp391.kpi.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Kiem tra luat dang nhap (UC-69). Repository va TokenService la "gia" (mock),
 * con BCrypt dung that de chac chan hash trong data.sql khop mat khau 123456.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // Dung hash dang co trong database/data.sql (= BCrypt cua "123456")
    private static final String HASH_OF_123456 = "$2a$10$zT84pKtOEfx1vO6h97AqFOHNBfoNNwgMDQAm5018jtLCVHsZU5HPm";

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, new BCryptPasswordEncoder(), tokenService);
    }

    private static User reporter(UserStatus status) {
        Role role = new Role();
        role.setId(1);
        role.setCode(RoleCode.REPORTER);
        role.setName("Phóng viên");

        User user = new User();
        user.setId(1);
        user.setUsername("an.nv");
        user.setEmail("an.nv@toasoan.vn");
        user.setPasswordHash(HASH_OF_123456);
        user.setFullName("Nguyễn Văn An");
        user.setRole(role);
        user.setSectionId(1);
        user.setStatus(status);
        return user;
    }

    private static LoginRequest request(String login, String password) {
        LoginRequest request = new LoginRequest();
        request.setLogin(login);
        request.setPassword(password);
        return request;
    }

    @Test
    void loginReturnsTokenAndUserInfoWhenPasswordIsCorrect() {
        when(userRepository.findByUsernameOrEmail("an.nv@toasoan.vn", "an.nv@toasoan.vn"))
                .thenReturn(Optional.of(reporter(UserStatus.ACTIVE)));
        when(tokenService.createToken(any(User.class))).thenReturn("token-gia");

        // co khoang trang thua o 2 dau van dang nhap duoc
        LoginResponse response = authService.login(request("  an.nv@toasoan.vn ", "123456"));

        assertEquals("token-gia", response.getToken());
        assertEquals(1, response.getId());
        assertEquals("REPORTER", response.getRoleCode());
        assertEquals("Phóng viên", response.getRoleName());
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userRepository.findByUsernameOrEmail("an.nv", "an.nv"))
                .thenReturn(Optional.of(reporter(UserStatus.ACTIVE)));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(request("an.nv", "1234567")));

        assertEquals(AuthService.WRONG_LOGIN_MESSAGE, ex.getMessage());
        verify(tokenService, never()).createToken(any(User.class));
    }

    @Test
    void loginRejectsUnknownAccountWithSameMessageAsWrongPassword() {
        when(userRepository.findByUsernameOrEmail("khong.co", "khong.co")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(request("khong.co", "123456")));

        assertEquals(AuthService.WRONG_LOGIN_MESSAGE, ex.getMessage());
    }

    @Test
    void loginRejectsLockedAccount() {
        when(userRepository.findByUsernameOrEmail("an.nv", "an.nv"))
                .thenReturn(Optional.of(reporter(UserStatus.LOCKED)));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(request("an.nv", "123456")));

        assertEquals(AuthService.LOCKED_MESSAGE, ex.getMessage());
        verify(tokenService, never()).createToken(any(User.class));
    }

    @Test
    void lockedAccountWithWrongPasswordDoesNotRevealLockedStatus() {
        when(userRepository.findByUsernameOrEmail("an.nv", "an.nv"))
                .thenReturn(Optional.of(reporter(UserStatus.LOCKED)));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(request("an.nv", "sai")));

        // Khong biet mat khau thi chi nhan cau chung, khong biet tai khoan bi khoa
        assertEquals(AuthService.WRONG_LOGIN_MESSAGE, ex.getMessage());
    }
}
