package com.swp391.kpi.controller;

import com.swp391.kpi.dto.ApiResponse;
import com.swp391.kpi.dto.CsvImportLogResponse;
import com.swp391.kpi.dto.CsvImportResultResponse;
import com.swp391.kpi.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * Controller xu ly API nhap so lieu tuong tac doc gia (UC-63) va nhat ky import.
 *
 * MUC LUC (so dong)
 *   33  field                        - service duoc inject
 *   38  uploadReaderCsv()            - Tai len file CSV so lieu doc gia (UC-63)
 *   54  getImportLogs()              - Danh sach nhat ky import co phan trang
 *   67  downloadTemplate()           - Tai file CSV mau de nhap lieu
 */
@RestController
@RequestMapping("/api/reader-imports")
@RequiredArgsConstructor
public class CsvImportController {

    private final CsvImportService csvImportService;

    /**
     * Tai len file CSV so lieu doc gia (UC-63, BR-KPI-03).
     * POST /api/reader-imports
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CsvImportResultResponse>> uploadReaderCsv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Integer currentUserId = Integer.valueOf(jwt.getSubject());
        CsvImportResultResponse result = csvImportService.importCsv(file, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Nhập số liệu độc giả từ CSV thành công.", result));
    }

    /**
     * Lay danh sach nhat ky import theo thoi gian moi nhat.
     * GET /api/reader-imports/logs?page=0&size=10
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<CsvImportLogResponse>>> getImportLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<CsvImportLogResponse> result = csvImportService.getImportLogs(pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách nhật ký import thành công.", result));
    }

    /**
     * Tai file CSV mau cho so lieu doc gia.
     * GET /api/reader-imports/template
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        String templateContent = csvImportService.getSampleCsvTemplate();
        byte[] bytes = templateContent.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reader_data_template.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
