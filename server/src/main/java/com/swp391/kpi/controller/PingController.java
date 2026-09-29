package com.swp391.kpi.controller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Kiem tra server song va noi duoc database.
 * Mo http://localhost:8080/api/ping -> {"trangThai":"OK","database":"swp391_kpi",...}
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PingController {

    private final JdbcTemplate jdbc;

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        String database = jdbc.queryForObject("SELECT DB_NAME()", String.class);
        return Map.of(
                "trangThai", "OK",
                "database", database,
                "thoiGian", LocalDateTime.now().toString());
    }
}
