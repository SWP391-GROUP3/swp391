package com.swp391.kpi.exception;

/**
 * Nem ra khi vi pham quy tac nghiep vu: sai trang thai, trung du lieu...
 * GlobalExceptionHandler bat loi nay va tra HTTP 409.
 * Vi du: throw new BusinessException("Kỳ nhuận bút đã khoá, không thể sửa");
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
