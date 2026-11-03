package com.swp391.kpi.assessment.controller;

import com.swp391.kpi.assessment.dto.AssignmentCreateRequest;
import com.swp391.kpi.assessment.dto.AssignmentResponse;
import com.swp391.kpi.assessment.dto.EditorWorkloadDTO;
import com.swp391.kpi.assessment.dto.PendingAssignmentResponse;
import com.swp391.kpi.assessment.service.AssignmentService;
import com.swp391.kpi.common.dto.ApiResponse;
import com.swp391.kpi.exception.NotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API phan cong bai cho BTV (UC-18...20): giao bai, xem phan cong theo bai / theo BTV, khoi luong viec
 * cua tung BTV.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   37  field                           service duoc inject
 *   43  getPendingAssignments()         Lay danh sach bai viet SUBMITTED cho Truong ban phan cong (UC-18) [GET /api/assignments/pending]
 *   54  assignArticle()                 Truong ban giao bai viet cho BTV cham chat luong (UC-18, UC-20) [POST /api/assignments]
 *   67  getAssignmentByArticleId()      Lay thong tin phan cong cua bai viet [GET /api/assignments/article/{articleId}]
 *   77  getAssignmentsByEditorId()      Lay danh sach bai viet duoc giao cua mot BTV (Hang viec BTV - UC-21) [GET /api/assignments/editor/{editorId}]
 *   86  getEditorWorkload()             Xem khoi luong cong viec cua mot BTV (so bai dang giu - UC-… [GET /api/assignments/editor/{editorId}/workload]
 */
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    /**
     * Lấy danh sách bài viết SUBMITTED cho Trưởng ban phân công (UC-18).
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<PendingAssignmentResponse>>> getPendingAssignments(
            @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<PendingAssignmentResponse> list = assignmentService.getPendingAssignments(currentUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài chờ phân công thành công", list));
    }

    /**
     * Trưởng ban giao bài viết cho BTV chấm chất lượng (UC-18, UC-20).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> assignArticle(@Valid @RequestBody AssignmentCreateRequest request,
                                                                     @AuthenticationPrincipal Jwt jwt) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        AssignmentResponse response = assignmentService.assignArticle(request, currentUserId);
        // M-B5: tao moi phan cong -> 201 (AGENTS.md muc 3.6)
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Phân công bài viết thành công", response));
    }

    /**
     * Lấy thông tin phân công của bài viết.
     */
    @GetMapping("/article/{articleId}")
    public ResponseEntity<ApiResponse<AssignmentResponse>> getAssignmentByArticleId(@PathVariable Integer articleId) {
        AssignmentResponse response = assignmentService.getAssignmentByArticleId(articleId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin phân công của bài viết này"));
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin phân công thành công", response));
    }

    /**
     * Lấy danh sách bài viết được giao của một BTV (Hàng việc BTV - UC-21).
     */
    @GetMapping("/editor/{editorId}")
    public ResponseEntity<ApiResponse<List<AssignmentResponse>>> getAssignmentsByEditorId(@PathVariable Integer editorId) {
        List<AssignmentResponse> list = assignmentService.getAssignmentsByEditorId(editorId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách bài được giao thành công", list));
    }

    /**
     * Xem khối lượng công việc của một BTV (số bài đang giữ - UC-19).
     */
    @GetMapping("/editor/{editorId}/workload")
    public ResponseEntity<ApiResponse<EditorWorkloadDTO>> getEditorWorkload(@PathVariable Integer editorId) {
        EditorWorkloadDTO workload = assignmentService.getEditorWorkload(editorId);
        return ResponseEntity.ok(ApiResponse.success("Lấy khối lượng công việc thành công", workload));
    }
}
