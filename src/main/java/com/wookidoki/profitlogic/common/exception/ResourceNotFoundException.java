package com.wookidoki.profitlogic.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, Long id) {
        super(resource + "을(를) 찾을 수 없습니다. (id: " + id + ")");
    }
}
