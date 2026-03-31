package myexpressionfriend_api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * LLM API 호출 한도 초과 예외 (HTTP 429)
 */
public class LlmQuotaExceededException extends BusinessException {

    private final Integer retryAfterSeconds;

    public LlmQuotaExceededException(String message, Integer retryAfterSeconds) {
        super(HttpStatus.TOO_MANY_REQUESTS, "LLM_QUOTA_EXCEEDED", message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public LlmQuotaExceededException(String message) {
        this(message, null);
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
