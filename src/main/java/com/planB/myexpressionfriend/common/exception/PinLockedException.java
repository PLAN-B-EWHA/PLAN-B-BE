package com.planB.myexpressionfriend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * PIN 연속 실패로 인해 잠금 상태일 때 발생하는 예외 (HTTP 423 Locked).
 */
public class PinLockedException extends BusinessException {

    public PinLockedException(String message) {
        super(HttpStatus.LOCKED, "PIN_LOCKED", message);
    }
}
