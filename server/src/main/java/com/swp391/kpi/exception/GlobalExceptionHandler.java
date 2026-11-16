package com.swp391.kpi.exception;

import com.swp391.kpi.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Bat loi chung cho moi controller, doi thanh ApiResponse (AGENTS.md muc 3.6).
 * Controller khong tu try/catch, cu de loi bay len day.
 * Loi khac (sai URL, sai method...) de Spring tu xu ly de giu dung status.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   39  hang so                         cau bao loi chung, ma cac annotation "bat buoc nhap"
 *   46  handleDataConflict()            xu ly data conflict
 *   52  handleStatus()                  xu ly status
 *   61  handleValidation()              @Valid tren Request DTO khong qua -> 400, moi o 1 cau loi, thu tu on dinh
 *   77  handleTypeMismatch()            Gia tri tren URL sai kieu (vd id khong phai so) -> 400
 *   84  handleUnreadableBody()          Body khong doc duoc (JSON hong, sai kieu du lieu) -> 400
 *   91  handleNotFound()                Khong tim thay du lieu -> 404
 *   98  handleUnauthorized()            Dang nhap that bai (sai tai khoan/mat khau, tai khoan bi khoa) -> 401
 *  105  handleBusiness()                Vi pham quy tac nghiep vu -> 409
 *  113  handleIllegalArgument()         Lop tinh toan thuan (ScoreCalculator, RoyaltyCalculator...) bao du lieu sai, vd diem < 50 thieu nhan xet -> 4…
 *  120  handleIllegalState()            Loi cau hinh he thong (vd thieu tham so trong system_parameters) -> 500, kem cau tieng Viet de bao Admin
 */
@RestControllerAdvice
@SuppressWarnings("unused") // Cac handler duoc Spring goi qua @ExceptionHandler.
public class GlobalExceptionHandler {

    private static final String SYSTEM_ERROR_MESSAGE = "Hệ thống gặp lỗi, vui lòng thử lại hoặc báo quản trị viên.";
    private static final String BAD_URL_VALUE_MESSAGE = "Tham số trên đường dẫn không hợp lệ.";
    private static final String UNREADABLE_BODY_MESSAGE = "Dữ liệu gửi lên không đọc được (sai định dạng JSON hoặc sai kiểu dữ liệu).";
    // Ten cac annotation "bat buoc nhap" (FieldError.getCode() tra ve ten annotation bi vi pham)
    private static final List<String> REQUIRED_CODES = List.of("NotBlank", "NotNull", "NotEmpty");

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

    // @Valid tren Request DTO khong qua -> 400, kem loi cua tung field.
    // 1 o co the vi pham nhieu luat cung luc (vd o rong: @NotBlank + @Pattern): giu cau gap dau tien,
    // rieng cau "bat buoc nhap" thi luon duoc uu tien -> o rong luon bao "khong duoc de trong".
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        // TreeMap xep cac o theo ten (a-z) nen thu tu loi giong nhau o moi lan goi
        Map<String, String> errors = new TreeMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String field = fieldError.getField();
            boolean isRequiredError = REQUIRED_CODES.contains(fieldError.getCode());
            if (!errors.containsKey(field) || isRequiredError) {
                errors.put(field, fieldError.getDefaultMessage());
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Dữ liệu không hợp lệ", errors));
    }

    // Gia tri tren URL sai kieu, vd /api/users/abc/lock (id phai la so) -> 400
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(BAD_URL_VALUE_MESSAGE, null));
    }

    // Body khong doc duoc: JSON hong hoac sai kieu du lieu, vd "sectionId": "abc" -> 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(UNREADABLE_BODY_MESSAGE, null));
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
