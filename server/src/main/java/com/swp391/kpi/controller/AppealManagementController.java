package com.swp391.kpi.controller;

import com.swp391.kpi.dto.*;
import com.swp391.kpi.service.AppealManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

/**
 * API Truong ban quan ly khieu nai (UC-36, UC-43): hang cho, chi tiet, BTV hop le, phan cong / giao
 * lai cham mu, giao cham lan 2.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   32  field                           service duoc inject
 *   35  getQueue()                      lay queue [GET /api/appeals]
 *   45  getDetail()                     lay detail [GET /api/appeals/{id}]
 *   52  getCandidates()                 lay candidates [GET /api/appeals/{id}/reviewer-candidates]
 *   59  assign()                        phan cong [POST /api/appeals/{id}/assign]
 *   66  reassign()                      phan cong lai [POST /api/appeals/{id}/reassign]
 *   73  assignSecond()                  phan cong second [POST /api/appeals/{id}/assign-second]
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/appeals")
public class AppealManagementController {
    private final AppealManagementService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AppealManagementResponse>>> getQueue(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status, @RequestParam(required = false) Integer articleId,
            @RequestParam(required = false) Integer reviewerId, @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khiếu nại thành công", service.getQueue(
                Integer.valueOf(jwt.getSubject()), status, articleId, reviewerId, fromDate, toDate, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppealManagementResponse>> getDetail(@PathVariable Integer id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết khiếu nại thành công",
                service.getDetail(id, Integer.valueOf(jwt.getSubject()))));
    }

    @GetMapping("/{id}/reviewer-candidates")
    public ResponseEntity<ApiResponse<List<AppealReviewerCandidateResponse>>> getCandidates(@PathVariable Integer id,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Lấy người chấm hợp lệ thành công",
                service.getCandidates(id, Integer.valueOf(jwt.getSubject()))));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<AppealManagementResponse>> assign(@PathVariable Integer id, // [LUONG 2 - PHAN CONG] buoc 4/6 controller
            @Valid @RequestBody AppealAssignRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Phân công chấm mù thành công",
                service.assign(id, request, Integer.valueOf(jwt.getSubject()))));
    }

    @PostMapping("/{id}/reassign")
    public ResponseEntity<ApiResponse<AppealManagementResponse>> reassign(@PathVariable Integer id,
            @Valid @RequestBody AppealReassignRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Giao lại phiếu chấm mù thành công",
                service.reassign(id, request, Integer.valueOf(jwt.getSubject()))));
    }

    @PostMapping("/{id}/assign-second")
    public ResponseEntity<ApiResponse<AppealManagementResponse>> assignSecond(@PathVariable Integer id, // [LUONG 4 - SO SANH & CHOT] nhanh S2 controller
            @Valid @RequestBody AppealAssignRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Phân công S2 thành công",
                service.assignSecond(id, request, Integer.valueOf(jwt.getSubject()))));
    }
}
