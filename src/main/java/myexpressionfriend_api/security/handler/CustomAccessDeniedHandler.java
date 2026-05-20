package myexpressionfriend_api.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.common.dto.common.ApiResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // 이미 응답이 커밋된 경우(SSE async dispatch 등) → 조용히 무시
        if (response.isCommitted()) {
            log.debug("Response already committed, skipping 403 write. URI={}", request.getRequestURI());
            return;
        }

        log.warn("Access denied - IP: {}, URL: {}, method: {}, message: {}",
                request.getRemoteAddr(), request.getRequestURI(),
                request.getMethod(), accessDeniedException.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error("접근 권한이 없습니다.", "ACCESS_DENIED"));
    }
}
