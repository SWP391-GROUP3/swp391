package com.swp391.kpi.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Endpoint kiem tra server song hay chua. Dung de client xac nhan da noi
 * duoc toi server truoc khi lam chuc nang that.
 *
 * <p>Xoa file nay khi da co controller that.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8081")
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "trangThai", "OK",
                "thongDiep", "Server KPI dang chay",
                "thoiGian", LocalDateTime.now().toString());
    }
}
