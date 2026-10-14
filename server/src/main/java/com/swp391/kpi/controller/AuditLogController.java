package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.AuditLogResponse;
import com.swp391.kpi.service.AppealAuditQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AppealAuditQueryService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getLogs(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) Integer articleId,
            @RequestParam(required = false) Integer appealId,
            @RequestParam(required = false) Integer editorId,
            @RequestParam(required = false) Integer actorId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Integer entityId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime
    ) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        List<AuditLogResponse> data = auditService.getLogs(
                currentUserId,
                actorId,
                actionType,
                entityType,
                entityId,
                fromTime,
                toTime,
                articleId,
                appealId,
                editorId
        );
        return ResponseEntity.ok(ApiResponse.success("Lấy nhật ký hệ thống thành công", data));
    }
}
