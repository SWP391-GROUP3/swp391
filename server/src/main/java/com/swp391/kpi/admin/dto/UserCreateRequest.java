package com.swp391.kpi.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Admin tao tai khoan moi. Do dai lay theo schema.sql: username VARCHAR(50),
 * email VARCHAR(150), full_name NVARCHAR(100).
 * Luat "vai tro nao phai co ban" kiem o UserAdminService (phu thuoc bang roles).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   23  hang so                         gioi han do dai (khong viet so ma trong code)
 *   29  field                           cac truong client gui len (kem kiem tra @NotBlank, @Size...)
 */
@Getter
@Setter
public class UserCreateRequest {

    // username la VARCHAR (khong dau) -> chi cho chu khong dau, so, dau cham, gach duoi, gach ngang
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9._-]+$";

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(max = 50, message = "Tên đăng nhập tối đa {max} ký tự")
    @Pattern(regexp = USERNAME_PATTERN,
            message = "Tên đăng nhập chỉ gồm chữ không dấu, số, dấu chấm, gạch dưới, gạch ngang")
    private String username;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa {max} ký tự")
    private String email;

    // Admin tu nhap mat khau ban dau; nguoi dung tu doi sau o trang ho so
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = UserPasswordChangeRequest.MIN_PASSWORD_LENGTH, max = UserPasswordChangeRequest.MAX_PASSWORD_LENGTH,
            message = "Mật khẩu phải từ {min} đến {max} ký tự")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa {max} ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng chọn vai trò")
    private String roleCode;        // REPORTER, EDITOR, ... (roles.code)

    private Integer sectionId;      // bat buoc voi PV / BTV / Truong ban, de null voi vai tro khac
}
