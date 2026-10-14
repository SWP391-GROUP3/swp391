package com.swp391.kpi.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDateTime;

/**
 * Tai khoan nguoi dung (bang users). Moi user dung 1 vai tro.
 * Mat khau chi luu BCrypt hash (passwordHash), KHONG luu mat khau tho.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;        // VARCHAR(50), UNIQUE

    private String email;           // VARCHAR(150), UNIQUE

    private String passwordHash;    // VARCHAR(255), BCrypt hash

    @Nationalized                   // cot NVARCHAR
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    // FK toi sections: entity Section chua co -> tam de Integer (AGENTS.md muc 3.3)
    private Integer sectionId;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private LocalDateTime createdAt;
}
