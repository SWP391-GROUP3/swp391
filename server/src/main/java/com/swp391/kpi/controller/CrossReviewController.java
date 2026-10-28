package com.swp391.kpi.controller;

import com.swp391.kpi.dto.*;
import com.swp391.kpi.service.CrossReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

/**
 * API cham cheo (UC-40): Truong ban tao / dong dot, go bai; BTV xem va nop phieu cham cheo.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   31  field                           service duoc inject
 *   34  batches()                       batches [GET /api/cross-review-batches]
 *   38  candidates()                    candidates [GET /api/cross-review-batches/candidates]
 *   43  batch()                         batch [GET /api/cross-review-batches/{id}]
 *   47  create()                        tao [POST /api/cross-review-batches]
 *   51  close()                         chot [POST /api/cross-review-batches/{id}/close]
 *   55  remove()                        xoa [DELETE /api/cross-review-batches/{id}/tasks/{taskId}]
 *   59  tasks()                         tasks [GET /api/cross-review-tasks]
 *   63  task()                          task [GET /api/cross-review-tasks/{id}]
 *   67  submit()                        nop [POST /api/cross-review-tasks/{id}/submit]
 */
@RestController @RequiredArgsConstructor
public class CrossReviewController {
    private final CrossReviewService service;

    @GetMapping("/api/cross-review-batches")
    public ResponseEntity<ApiResponse<List<CrossReviewBatchResponse>>> batches(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đợt chấm chéo thành công",service.getBatches(Integer.valueOf(jwt.getSubject()))));
    }
    @GetMapping("/api/cross-review-batches/candidates")
    public ResponseEntity<ApiResponse<List<CrossReviewCandidateResponse>>> candidates(@AuthenticationPrincipal Jwt jwt,
            @RequestParam LocalDate fromDate, @RequestParam LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success("Lấy bài và người chấm hợp lệ thành công",service.getCandidates(Integer.valueOf(jwt.getSubject()),fromDate,toDate)));
    }
    @GetMapping("/api/cross-review-batches/{id}")
    public ResponseEntity<ApiResponse<CrossReviewBatchResponse>> batch(@PathVariable Integer id,@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đợt thành công",service.getBatch(id,Integer.valueOf(jwt.getSubject()))));
    }
    @PostMapping("/api/cross-review-batches")
    public ResponseEntity<ApiResponse<CrossReviewBatchResponse>> create(@Valid @RequestBody CrossReviewBatchCreateRequest request,@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(201).body(ApiResponse.success("Tạo đợt chấm chéo thành công",service.create(request,Integer.valueOf(jwt.getSubject()))));
    }
    @PostMapping("/api/cross-review-batches/{id}/close")
    public ResponseEntity<ApiResponse<CrossReviewBatchResponse>> close(@PathVariable Integer id,@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Đóng đợt chấm chéo thành công",service.close(id,Integer.valueOf(jwt.getSubject()))));
    }
    @DeleteMapping("/api/cross-review-batches/{id}/tasks/{taskId}")
    public ResponseEntity<ApiResponse<CrossReviewBatchResponse>> remove(@PathVariable Integer id,@PathVariable Integer taskId,@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Gỡ bài chấm chéo thành công",service.removeTask(id,taskId,Integer.valueOf(jwt.getSubject()))));
    }
    @GetMapping("/api/cross-review-tasks")
    public ResponseEntity<ApiResponse<List<AppealReviewTaskResponse>>> tasks(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Lấy bài chấm chéo thành công",service.getMyTasks(Integer.valueOf(jwt.getSubject()))));
    }
    @GetMapping("/api/cross-review-tasks/{id}")
    public ResponseEntity<ApiResponse<AppealBlindScorecardResponse>> task(@PathVariable Integer id,@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Lấy phiếu chấm chéo thành công",service.getTask(id,Integer.valueOf(jwt.getSubject()))));
    }
    @PostMapping("/api/cross-review-tasks/{id}/submit")
    public ResponseEntity<ApiResponse<AppealBlindScorecardResponse>> submit(@PathVariable Integer id,
            @Valid @RequestBody AppealScorecardRequest request,@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(ApiResponse.success("Nộp chấm chéo thành công",service.submit(id,Integer.valueOf(jwt.getSubject()),request)));
    }
}
