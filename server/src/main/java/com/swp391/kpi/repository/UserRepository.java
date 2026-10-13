package com.swp391.kpi.repository;

import com.swp391.kpi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Doc bang users. Luong khac can thong tin user thi goi service, khong goi thang repository nay.
 */
public interface UserRepository extends JpaRepository<User, Integer> {

    // Dang nhap bang username HOAC email (UC-69 ghi username, man Login dung email -> nhan ca 2).
    // Spring tu sinh: SELECT ... WHERE username = ? OR email = ?
    Optional<User> findByUsernameOrEmail(String username, String email);
}
