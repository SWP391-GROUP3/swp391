package com.swp391.kpi.controller;

import com.swp391.kpi.dto.AssignmentCreateRequest;
import com.swp391.kpi.dto.AssignmentResponse;
import com.swp391.kpi.dto.EditorWorkloadDTO;
import com.swp391.kpi.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    /**
     * Trưởng ban giao bài viết cho BTV chấm chất lượng (UC-18, UC-20).
     */
    @PostMapping
    public ResponseEntity<?> assignArticle(@RequestBody AssignmentCreateRequest request) {
        try {
            AssignmentResponse response = assignmentService.assignArticle(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Lấy thông tin phân công của bài viết.
     */
    @GetMapping("/article/{articleId}")
    public ResponseEntity<?> getAssignmentByArticleId(@PathVariable Integer articleId) {
        return assignmentService.getAssignmentByArticleId(articleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lấy danh sách bài viết được giao của một BTV (Hàng việc BTV - UC-21).
     */
    @GetMapping("/editor/{editorId}")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentsByEditorId(@PathVariable Integer editorId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByEditorId(editorId));
    }

    /**
     * Xem khối lượng công việc của một BTV (số bài đang giữ - UC-19).
     */
    @GetMapping("/editor/{editorId}/workload")
    public ResponseEntity<EditorWorkloadDTO> getEditorWorkload(@PathVariable Integer editorId) {
        return ResponseEntity.ok(assignmentService.getEditorWorkload(editorId));
    }
}
