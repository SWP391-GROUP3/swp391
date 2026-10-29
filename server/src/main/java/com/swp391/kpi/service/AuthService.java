package com.swp391.kpi.service;

import com.swp391.kpi.dto.LoginRequest;
import com.swp391.kpi.dto.LoginResponse;
import com.swp391.kpi.entity.User;
import com.swp391.kpi.entity.UserStatus;
import com.swp391.kpi.exception.UnauthorizedException;
import com.swp391.kpi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Dang nhap (UC-69). Dang xuat (UC-70) lam o frontend: xoa token la xong,
 * vi server khong luu phien (STATELESS).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   31  hang so                         ten tham so, gioi han... (khong viet so ma trong code)
 *   34  field                           repository / service duoc inject
 *   39  login()                         dang nhap
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    // Sai tai khoan hay sai mat khau deu bao CUNG 1 cau: khong cho nguoi ngoai
    // do xem email/username nao co trong he thong.
    static final String WRONG_LOGIN_MESSAGE = "Email/tên đăng nhập hoặc mật khẩu không đúng.";
    static final String LOCKED_MESSAGE = "Tài khoản đã bị khoá. Vui lòng liên hệ quản trị viên.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String login = request.getLogin().trim();

        // 1. Tim theo username hoac email
        Optional<User> found = userRepository.findByUsernameOrEmail(login, login);
        if (found.isEmpty()) {
            throw new UnauthorizedException(WRONG_LOGIN_MESSAGE);
        }
        User user = found.get();

        // 2. So mat khau go vao voi hash BCrypt trong DB (BCrypt tu lay "muoi" trong hash ra de so)
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException(WRONG_LOGIN_MESSAGE);
        }

        // 3. Chi tai khoan ACTIVE moi duoc vao (UC-69). Kiem SAU mat khau:
        //    ai khong biet mat khau thi khong biet duoc tai khoan co bi khoa hay khong.
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException(LOCKED_MESSAGE);
        }

        // 4. Tao token, tra kem thong tin user
        String token = tokenService.createToken(user);
        return LoginResponse.from(user, token);
    }
}
