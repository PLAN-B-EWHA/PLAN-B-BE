package com.planB.myexpressionfriend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 중복 등록, 한도 초과 등 현재 상태와 충돌하는 요청 시 발생하는 예외 (HTTP 409).
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
