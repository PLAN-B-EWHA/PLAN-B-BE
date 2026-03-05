package com.planB.myexpressionfriend.common.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planB.myexpressionfriend.common.config.JWTProperties;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.util.JWTUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class APILoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JWTUtil jwtUtil;
    private final JWTProperties jwtProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        log.info("============= 로그인 성공 =============");

        UserDTO userDTO = (UserDTO) authentication.getPrincipal();
        log.info("로그인 사용자: {}", userDTO.getEmail());
        log.info("사용자 권한: {}", userDTO.getRoles());

        Map<String, Object> claims = userDTO.getClaims();

        String accessToken = jwtUtil.generateToken(
                claims,
                jwtProperties.getAccessTokenExpireMinutes()
        );
        String refreshToken = jwtUtil.generateToken(
                claims,
                jwtProperties.getRefreshTokenExpireMinutes()
        );

        log.info("Access Token 생성 완료 (만료: {}분)", jwtProperties.getAccessTokenExpireMinutes());
        log.info("Refresh Token 생성 완료 (만료: {}분)", jwtProperties.getRefreshTokenExpireMinutes());

        addRefreshTokenCookie(response, refreshToken);

        Map<String, Object> responseMap = Map.of(
                "accessToken", accessToken,
                "userId", userDTO.getUserId().toString(),
                "email", userDTO.getEmail(),
                "name", userDTO.getName(),
                "roles", userDTO.getRoles()
        );

        log.info("로그인 응답 전송");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), responseMap);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(
                        jwtProperties.getCookie().getName(),
                        refreshToken
                )
                .httpOnly(jwtProperties.getCookie().isHttpOnly())
                .secure(jwtProperties.getCookie().isSecure())
                .path(jwtProperties.getCookie().getPath())
                .maxAge(jwtProperties.getCookie().getMaxAge())
                .sameSite(jwtProperties.getCookie().getSameSite())
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        log.info(
                "Refresh Token 쿠키 설정: name={}, httpOnly={}, secure={}, maxAge={}s",
                jwtProperties.getCookie().getName(),
                jwtProperties.getCookie().isHttpOnly(),
                jwtProperties.getCookie().isSecure(),
                jwtProperties.getCookie().getMaxAge()
        );
    }
}
