package com.swp391.kpi.dto;

import com.swp391.kpi.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Ho so cua nguoi dang dang nhap (man "Hồ sơ của tôi").
 * KHONG tra passwordHash.
 */
@Getter
@Setter
public class UserProfileResponse {

    private Integer id;
    private String username;
    private String email;
    private String fullName;
    private String roleCode;    // REPORTER, ADMIN...
    private String roleName;    // "Phóng viên"... de hien len man hinh
    private Integer sectionId;  // null voi vai tro khong thuoc ban
    private String status;      // ACTIVE / LOCKED
    private LocalDateTime createdAt;

    // Goi trong transaction cua service (role la LAZY)
    public static UserProfileResponse from(User user) {
        UserProfileResponse dto = new UserProfileResponse();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setRoleCode(user.getRole().getCode().name());
        dto.setRoleName(user.getRole().getName());
        dto.setSectionId(user.getSectionId());
        dto.setStatus(user.getStatus().name());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
