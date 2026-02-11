package com.wookidoki.profitlogic.common.exception;

public class UnauthorizedAccessException extends RuntimeException {

    public UnauthorizedAccessException() {
        super("해당 리소스에 접근 권한이 없습니다.");
    }

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
