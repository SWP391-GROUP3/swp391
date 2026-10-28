package com.swp391.kpi.exception;

/**
 * Nem ra khi tim theo id ma khong thay du lieu.
 * GlobalExceptionHandler bat loi nay va tra HTTP 404.
 * Vi du: throw new NotFoundException("Không tìm thấy bài viết");
 *
 * MUC LUC (so dong - ten - tac dung)
 *   13  NotFoundException()             ham khoi tao
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
