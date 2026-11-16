package com.swp391.kpi.admin.controller;

import com.swp391.kpi.admin.dto.SectionResponse;
import com.swp391.kpi.admin.service.UserAdminService;
import com.swp391.kpi.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Danh sach ban dang hoat dong (o chon ban tren form tao tai khoan).
 * Ai dang nhap cung doc duoc: ten ban khong phai du lieu nhay cam.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   27  field                           service duoc inject
 *   31  getActiveSections()             cac ban dang hoat dong [GET /api/sections]
 */
@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
public class SectionController {

    private final UserAdminService userAdminService;

    // GET /api/sections
    @GetMapping
    public ResponseEntity<ApiResponse<List<SectionResponse>>> getActiveSections() {
        List<SectionResponse> data = userAdminService.getActiveSections();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách ban thành công", data));
    }
}
