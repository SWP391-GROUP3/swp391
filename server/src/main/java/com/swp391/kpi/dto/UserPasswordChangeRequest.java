package com.swp391.kpi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Nguoi dung tu doi mat khau: phai nhap dung mat khau hien tai.
 */
@Getter
@Setter
public class UserPasswordChangeRequest {

    public static final int MIN_PASSWORD_LENGTH = 6;
    // BCrypt chi doc 72 byte dau, dai hon thi phan thua bi bo qua
    public static final int MAX_PASSWORD_LENGTH = 72;

    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
    private String currentPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(min = MIN_PASSWORD_LENGTH, max = MAX_PASSWORD_LENGTH,
            message = "Mật khẩu mới phải từ {min} đến {max} ký tự")
    private String newPassword;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
    private String confirmPassword;
}
