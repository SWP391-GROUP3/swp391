package com.swp391.kpi.admin.repository;

import com.swp391.kpi.admin.entity.User;
import com.swp391.kpi.admin.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Doc bang users. Luong khac can thong tin user thi goi service, khong goi thang repository nay.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   23  findByUsernameOrEmail()         Dang nhap bang username HOAC email (UC-69 ghi username, man Login dung email -> nhan ca 2)
 *   27  existsByEmailAndIdNot()         Sua ho so: email moi da co NGUOI KHAC dung chua (bo qua chinh minh)
 *   31  existsByUsername()              Admin tao tai khoan: username da co ai dung chua
 *   33  existsByEmail()                 Admin tao tai khoan: email da co ai dung chua
 *   37  existsByIdAndStatus()           Tai khoan co dung trang thai nay khong (kiem token cua tai khoan da khoa)
 */
public interface UserRepository extends JpaRepository<User, Integer> {

    // Dang nhap bang username HOAC email (UC-69 ghi username, man Login dung email -> nhan ca 2).
    // Spring tu sinh: SELECT ... WHERE username = ? OR email = ?
    Optional<User> findByUsernameOrEmail(String username, String email);

    // Sua ho so: email moi da co NGUOI KHAC dung chua (bo qua chinh minh).
    // Spring tu sinh: SELECT ... WHERE email = ? AND id <> ?
    boolean existsByEmailAndIdNot(String email, Integer id);

    // Admin tao tai khoan: username / email da co ai dung chua.
    // Spring tu sinh: SELECT ... WHERE username = ? (email = ?)
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Moi request co token: tai khoan nay con dung trang thai do khong (SecurityConfig hoi qua AuthService).
    // Spring tu sinh: SELECT ... WHERE id = ? AND status = ?
    boolean existsByIdAndStatus(Integer id, UserStatus status);
}
