package com.swp391.kpi.exception;

/**
 * Nem ra khi dang nhap that bai: sai tai khoan / mat khau, tai khoan bi khoa.
 * GlobalExceptionHandler bat loi nay va tra HTTP 401.
 *
 * MUC LUC (so dong - ten - tac dung)
 *   12  UnauthorizedException()         ham khoi tao
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
