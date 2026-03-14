package com.planB.myexpressionfriend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 입력값이 비즈니스 규칙에 맞지 않을 때 발생하는 예외 (HTTP 400).
 *
 * <p>Bean Validation(@Valid) 오류와는 별개로, 서비스 레이어에서 직접 검증하는 경우에 사용합니다.</p>
 */
public class InvalidRequestException extends BusinessException {

    public InvalidRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
    }
}
