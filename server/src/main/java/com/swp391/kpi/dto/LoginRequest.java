package com.swp391.kpi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Body cua POST /api/auth/login.
 * login = email HOAC username (ca 2 cot deu UNIQUE trong bang users).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Vui lòng nhập email hoặc tên đăng nhập")
    private String login;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String password;
}
