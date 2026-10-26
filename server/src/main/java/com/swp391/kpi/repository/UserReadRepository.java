package com.swp391.kpi.repository;

import com.swp391.kpi.dto.UserAccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * MUC LUC (so dong)
 *   17  field                        - cac truong du lieu
 *   19  findAll()
 */
@Repository
@RequiredArgsConstructor
public class UserReadRepository {
    private final JdbcTemplate jdbcTemplate;

    public List<UserAccessResponse> findAll() {
        return jdbcTemplate.query("""
                SELECT u.id, u.full_name, r.code, u.section_id, s.name AS section_name, u.status
                FROM users u JOIN roles r ON r.id = u.role_id
                LEFT JOIN sections s ON s.id = u.section_id
                """, (row, index) -> new UserAccessResponse(row.getInt("id"), row.getString("full_name"),
                row.getString("code"), row.getObject("section_id", Integer.class),
                row.getString("section_name"), row.getString("status")));
    }
}
