package com.planB.myexpressionfriend.common.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;  // Jackson

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {

        log.error("============= 접근 거부 =============");
        log.error("요청 IP: {}", request.getRemoteAddr());
        log.error("요청 URL: {}", request.getRequestURI());
        log.error("요청 메서드: {}", request.getMethod());
        log.error("에러 메시지: {}", accessDeniedException.getMessage());

        // JSON 응답 생성
        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", "ERROR_ACCESSDENIED",
                "message", "접근 권한이 없습니다",
                "path", request.getRequestURI(),
                "timestamp", System.currentTimeMillis()
        );

        // 응답 설정
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);  // 403
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Jackson으로 JSON 변환 및 응답
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
