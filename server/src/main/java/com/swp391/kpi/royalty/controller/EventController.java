package com.swp391.kpi.royalty.controller;

import com.swp391.kpi.common.dto.ApiResponse;
import com.swp391.kpi.royalty.dto.EventResponse;
import com.swp391.kpi.royalty.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API su kien (BF-04) cho o chon su kien o man soan/nop bai cua BF-01 (BR-IN-03).
 * Moi nguoi da dang nhap deu goi duoc (SecurityConfig: anyRequest().authenticated()).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   27  field                           service duoc inject
 *   31  getOpenEvents()                 lay open events [GET /api/events/open]
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // GET /api/events/open: su kien dang mo hom nay
    @GetMapping("/open")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getOpenEvents() {
        List<EventResponse> data = eventService.getOpenEvents();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sự kiện đang mở thành công", data));
    }
}
