package com.planB.myexpressionfriend.common.exception;

import lombok.Getter;

@Getter
public class LlmQuotaExceededException extends RuntimeException {

    private final Integer retryAfterSeconds;

    public LlmQuotaExceededException(String message, Integer retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
