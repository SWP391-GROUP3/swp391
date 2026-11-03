package com.swp391.kpi.common.dto;

import com.swp391.kpi.admin.entity.User;
import lombok.Getter;
import lombok.Setter;

/**
 * Tra ve khi dang nhap thanh cong: token + thong tin de frontend hien ten, vai tro.
 * KHONG tra passwordHash.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  field                           cac truong tra ve cho giao dien
 *   29  from()                          Goi trong transaction cua service (role la LAZY, can load truoc khi ra khoi service)
 */
@Getter
@Setter
public class LoginResponse {

    private String token;       // gui kem moi request: "Authorization: Bearer <token>"
    private Integer id;
    private String username;
    private String email;
    private String fullName;
    private String roleCode;    // REPORTER, ADMIN... (frontend dung de chon trang)
    private String roleName;    // "Phong vien"... (hien len man hinh)
    private Integer sectionId;

    // Goi trong transaction cua service (role la LAZY, can load truoc khi ra khoi service)
    public static LoginResponse from(User user, String token) {
        LoginResponse dto = new LoginResponse();
        dto.setToken(token);
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRoleCode(user.getRole().getCode().name());
        dto.setRoleName(user.getRole().getName());
        dto.setSectionId(user.getSectionId());
        return dto;
    }
}
