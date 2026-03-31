package myexpressionfriend_api.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 인증되지 않은 사용자의 요청 시 발생하는 예외 (HTTP 401)
 *
 * <p>토큰 없음, 토큰 만료, 로그인 실패처럼 요청자가 누구인지
 * 확인할 수 없을 때 서비스 레이어에서 사용</p>
 */
public class AuthenticationFailedException extends BusinessException {
    public AuthenticationFailedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }
}
