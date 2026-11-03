package com.swp391.kpi.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Nguoi dung tu sua ho so: chi ho ten va email.
 * Vai tro, ban, trang thai, username do Admin quan ly -> khong nhan o day.
 * Do dai lay theo schema.sql: full_name NVARCHAR(100), email VARCHAR(150).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   23  field                           cac truong client gui len (kem kiem tra @NotNull, @Size...)
 */
@Getter
@Setter
public class UserProfileUpdateRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên tối đa {max} ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa {max} ký tự")
    private String email;
}
