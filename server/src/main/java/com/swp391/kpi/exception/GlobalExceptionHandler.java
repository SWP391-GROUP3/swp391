package com.swp391.kpi.exception;

import com.swp391.kpi.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Bat loi chung cho moi controller, doi thanh ApiResponse (AGENTS.md muc 3.6).
 * Controller khong tu try/catch, cu de loi bay len day.
 * Loi khac (sai URL, sai method...) de Spring tu xu ly de giu dung status.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   34  handleDataConflict()            xu ly data conflict
 *   40  handleStatus()                  xu ly status
 *   47  handleValidation()              @Valid tren Request DTO khong qua -> 400, kem loi cua tung field
 *   58  handleNotFound()                Khong tim thay du lieu -> 404
 *   65  handleUnauthorized()            Dang nhap that bai (sai tai khoan/mat khau, tai khoan bi khoa) -> 401
 *   72  handleBusiness()                Vi pham quy tac nghiep vu -> 409
 *   81  handleIllegalArgument()         Lop tinh toan bao du lieu sai -> 400
 *   88  handleIllegalState()            Loi cau hinh he thong (thieu tham so...) -> 500 kem cau tieng Viet
 */
@RestControllerAdvice
@SuppressWarnings("unused") // Cac handler duoc Spring goi qua @ExceptionHandler.
public class GlobalExceptionHandler {

    private static final String SYSTEM_ERROR_MESSAGE = "Hệ thống gặp lỗi, vui lòng thử lại hoặc báo quản trị viên.";

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataConflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Dữ liệu bị trùng hoặc không còn hợp lệ. Vui lòng tải lại và thử lại.", null));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleStatus(org.springframework.web.server.ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(ApiResponse.error(ex.getReason(), null));
    }

    // @Valid tren Request DTO khong qua -> 400, kem loi cua tung field
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Dữ liệu không hợp lệ", errors));
    }

    // Khong tim thay du lieu -> 404
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), null));
    }

    // Dang nhap that bai (sai tai khoan/mat khau, tai khoan bi khoa) -> 401
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), null));
    }

    // Vi pham quy tac nghiep vu -> 409
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), null));
    }

    // Lop tinh toan thuan (ScoreCalculator, RoyaltyCalculator...) bao du lieu sai, vd diem < 50 thieu nhan xet -> 400.
    // Truoc day loi nay ra 500 trang, nguoi dung khong biet sai o dau.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), null));
    }

    // Loi cau hinh he thong (vd thieu tham so trong system_parameters) -> 500, kem cau tieng Viet de bao Admin.
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : SYSTEM_ERROR_MESSAGE;
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message, null));
    }
}
