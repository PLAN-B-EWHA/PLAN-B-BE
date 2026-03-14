package com.planB.myexpressionfriend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 요청한 리소스를 찾을 수 없을 때 발생하는 예외 (HTTP 404).
 */
public class EntityNotFoundException extends BusinessException {

    public EntityNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "ENTITY_NOT_FOUND", message);
    }
}
