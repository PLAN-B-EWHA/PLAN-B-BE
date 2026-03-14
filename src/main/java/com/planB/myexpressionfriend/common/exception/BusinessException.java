package com.planB.myexpressionfriend.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 로직 예외의 최상위 기반 클래스.
 *
 * <p>HTTP 상태 코드와 클라이언트 식별용 errorCode를 함께 가집니다.
 * GlobalExceptionHandler가 이 클래스를 감지해 적절한 HTTP 응답을 생성합니다.</p>
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    protected BusinessException(HttpStatus httpStatus, String errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }
}
