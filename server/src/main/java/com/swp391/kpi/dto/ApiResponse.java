package com.swp391.kpi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.Map;

/**
 * Khung chung cho moi response cua API (xem CODING_PLAN muc 1.5).
 * Thanh cong: { success, message, data }. Loi: { success, message, errors }.
 *
 * MUC LUC (so dong)
 *   22  field                        - cac truong tra ve cho giao dien
 *   27  ApiResponse()
 *   34  success()
 *   38  error()
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)   // field null thi khong in ra JSON
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final Map<String, String> errors;

    private ApiResponse(boolean success, String message, T data, Map<String, String> errors) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errors = errors;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> error(String message, Map<String, String> errors) {
        return new ApiResponse<>(false, message, null, errors);
    }
}
