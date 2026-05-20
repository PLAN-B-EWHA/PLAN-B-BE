package myexpressionfriend_api.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 요청에 대한 401 응답 처리
 * <p>
 * SSE(Server-Sent Events) 등 비동기 요청에서 응답이 이미 committed된 경우
 * 조용히 무시하여 "Unable to handle the Spring Security Exception because the
 * response is already committed." 로그 폭발을 방지합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        // 이미 응답이 커밋된 경우(SSE async dispatch 등) → 조용히 무시
        if (response.isCommitted()) {
            log.debug("Response already committed, skipping 401 write. URI={}", request.getRequestURI());
            return;
        }

        log.warn("Unauthorized access - URI: {}, message: {}", request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error("인증이 필요합니다.", "UNAUTHORIZED"));
    }
}
