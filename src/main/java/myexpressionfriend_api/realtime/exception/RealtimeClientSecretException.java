package myexpressionfriend_api.realtime.exception;

import myexpressionfriend_api.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class RealtimeClientSecretException extends BusinessException {

    public RealtimeClientSecretException(HttpStatus httpStatus, String errorCode, String message) {
        super(httpStatus, errorCode, message);
    }
}
