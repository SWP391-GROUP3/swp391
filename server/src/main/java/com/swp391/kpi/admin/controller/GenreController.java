package com.swp391.kpi.admin.controller;

import com.swp391.kpi.admin.dto.GenreResponse;
import com.swp391.kpi.admin.service.GenreService;
import com.swp391.kpi.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API doc the loai bai (luong admin). Moi vai tro da dang nhap deu goi duoc
 * (SecurityConfig: anyRequest().authenticated()), vd o chon the loai o man soan bai (UC-03).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   27  field                           service duoc inject
 *   31  getActiveGenres()               lay active genres [GET /api/genres]
 */
@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreService genreService;

    // GET /api/genres: cac the loai dang dung, xep theo ten
    @GetMapping
    public ResponseEntity<ApiResponse<List<GenreResponse>>> getActiveGenres() {
        List<GenreResponse> data = genreService.getActiveGenres();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thể loại thành công", data));
    }
}
