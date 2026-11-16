package com.swp391.kpi.admin.repository;

import com.swp391.kpi.admin.entity.Role;
import com.swp391.kpi.admin.entity.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Doc bang roles (vai tro co dinh).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   19  findByCode()                    Tim vai tro theo ma (REPORTER, EDITOR...) khi Admin tao tai khoan
 */
public interface RoleRepository extends JpaRepository<Role, Integer> {

    // Tim vai tro theo ma. Spring tu sinh: SELECT ... FROM roles WHERE code = ?
    Optional<Role> findByCode(RoleCode code);
}
