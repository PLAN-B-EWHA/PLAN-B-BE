package com.planB.myexpressionfriend.common.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class APILoginFailHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;  // Jackson

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        log.error("요청 IP: {}", request.getRemoteAddr());
        log.error("요청 URL: {}", request.getRequestURI(), request.getRemoteAddr());
        log.error("오류 메시지: {}", exception.getMessage(), request.getRequestURI());
        log.error("오류 메시지: {}", exception.getMessage());

        // JSON ?묐떟 ?앹꽦
        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", "ERROR_LOGIN",
                "message", "이메일 또는 비밀번호가 올바르지 않습니다.",
                "timestamp", System.currentTimeMillis()
        );

        // ?묐떟 ?ㅼ젙
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);  // 401
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
