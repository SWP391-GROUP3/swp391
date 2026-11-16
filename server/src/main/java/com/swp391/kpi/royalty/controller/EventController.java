package com.swp391.kpi.royalty.controller;

import com.swp391.kpi.common.dto.ApiResponse;
import com.swp391.kpi.royalty.dto.EventResponse;
import com.swp391.kpi.royalty.dto.EventSaveRequest;
import com.swp391.kpi.royalty.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API su kien bao hoa (BF-04).
 *  - GET /api/events/open: o chon su kien o man soan/nop bai cua BF-01 (BR-IN-03), moi nguoi da dang nhap goi duoc.
 *  - Cac API con lai: man quan ly su kien, chi Thu ky toa soan (luat trong SecurityConfig).
 *
 * MUC LUC (so dong - ten - tac dung)
 *   44  field                           service duoc inject
 *   48  getOpenEvents()                 lay su kien dang mo hom nay [GET /api/events/open]
 *   55  getAllEvents()                  lay tat ca su kien [GET /api/events]
 *   62  createEvent()                   tao su kien [POST /api/events]
 *   73  updateEvent()                   sua su kien [PUT /api/events/{id}]
 *   82  activateEvent()                 bat su kien [POST /api/events/{id}/activate]
 *   89  deactivateEvent()               tat su kien [POST /api/events/{id}/deactivate]
 *   96  deleteEvent()                   xoa su kien [DELETE /api/events/{id}]
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

    // GET /api/events: tat ca su kien, moi nhat len dau
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> getAllEvents() {
        List<EventResponse> data = eventService.getAllEvents();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sự kiện thành công", data));
    }

    // POST /api/events: tao su kien moi
    @PostMapping
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(
            @Valid @RequestBody EventSaveRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());   // id nguoi dang nhap, lay tu token
        EventResponse data = eventService.createEvent(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo sự kiện thành công", data));
    }

    // PUT /api/events/{id}: sua su kien
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable Integer id,
            @Valid @RequestBody EventSaveRequest request) {
        EventResponse data = eventService.updateEvent(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sự kiện thành công", data));
    }

    // POST /api/events/{id}/activate: bat lai su kien
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<EventResponse>> activateEvent(@PathVariable Integer id) {
        EventResponse data = eventService.setEventActive(id, true);
        return ResponseEntity.ok(ApiResponse.success("Đã bật sự kiện", data));
    }

    // POST /api/events/{id}/deactivate: tat su kien
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<EventResponse>> deactivateEvent(@PathVariable Integer id) {
        EventResponse data = eventService.setEventActive(id, false);
        return ResponseEntity.ok(ApiResponse.success("Đã tắt sự kiện", data));
    }

    // DELETE /api/events/{id}: xoa su kien chua co bai nao gan vao
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable Integer id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xoá sự kiện", null));
    }
}
