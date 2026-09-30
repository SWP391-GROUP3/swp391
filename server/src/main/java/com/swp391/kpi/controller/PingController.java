package com.swp391.kpi.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.RequiredArgsConstructor;

/**
 * Endpoint kiem tra server song hay chua. Dung de client xac nhan da noi
 * duoc toi server truoc khi lam chuc nang that.
 *
 * <p>Kiem tra ca database, mo http://localhost:8080/api/ping -> {"trangThai":"OK","database":"swp391_kpi",...}
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8081")
@RequiredArgsConstructor
public class PingController {

    private final JdbcTemplate jdbc;

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        String database = jdbc.queryForObject("SELECT DB_NAME()", String.class);
        return Map.of(
                "trangThai", "OK",
                "database", database,
                "thongDiep", "Server KPI dang chay",
                "thoiGian", LocalDateTime.now().toString());
    }
}
