package myexpressionfriend_api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 현재 상태와 충돌하는 요청 시 발생하는 예외 (HTTP 409)
 *
 * <p>중복 등록, 한도 초과처럼 요청 자체는 유효하지만
 * 서버의 현재 상태와 충돌할 때 서비스 레이어에서 사용</p>
 */
public class ConflictException extends BusinessException{

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
