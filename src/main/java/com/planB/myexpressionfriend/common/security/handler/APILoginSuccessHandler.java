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
    private final ObjectMapper objectMapper;  // Jackson

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("============= 로그인 성공 =============");

        // 인증된 사용자 정보 가져오기
        UserDTO userDTO = (UserDTO) authentication.getPrincipal();

        log.info("로그인 사용자: {}", userDTO.getEmail());
        log.info("사용자 역할: {}", userDTO.getRoles());

        // JWT에 저장할 정보 (Claims)
        Map<String, Object> claims = userDTO.getClaims();

        // Access Token 생성 (application.properties에서 시간 가져옴)
        String accessToken = jwtUtil.generateToken(
                claims,
                jwtProperties.getAccessTokenExpireMinutes()
        );

        // Refresh Token 생성
        String refreshToken = jwtUtil.generateToken(
                claims,
                jwtProperties.getRefreshTokenExpireMinutes()
        );

        log.info("Access Token 생성 완료 (만료: {}분)", jwtProperties.getAccessTokenExpireMinutes());
        log.info("Refresh Token 생성 완료 (만료: {}분)", jwtProperties.getRefreshTokenExpireMinutes());

        // Refresh Token을 HttpOnly 쿠키에 저장 (보안 강화)
        addRefreshTokenCookie(response, refreshToken);

        // JSON 응답 생성 (Jackson 사용)
        Map<String, Object> responseMap = Map.of(
                "accessToken", accessToken,
                // refreshToken은 쿠키에 저장되므로 응답에서 제외 (보안)
                "userId", userDTO.getUserId().toString(),
                "email", userDTO.getEmail(),
                "name", userDTO.getName(),
                "roles", userDTO.getRoles()
        );

        log.info("로그인 응답 전송");

        // 응답 설정
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        // Jackson으로 JSON 변환 및 응답
        objectMapper.writeValue(response.getWriter(), responseMap);
    }

    /**
     * Refresh Token을 HttpOnly 쿠키에 추가
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {

        // Spring 6.1+ ResponseCookie 사용 (권장)
        ResponseCookie cookie = ResponseCookie.from(
                        jwtProperties.getCookie().getName(),
                        refreshToken
                )
                .httpOnly(jwtProperties.getCookie().isHttpOnly())  // XSS 공격 방지
                .secure(jwtProperties.getCookie().isSecure())      // HTTPS만 허용 (프로덕션)
                .path(jwtProperties.getCookie().getPath())
                .maxAge(jwtProperties.getCookie().getMaxAge())
                .sameSite(jwtProperties.getCookie().getSameSite()) // CSRF 공격 방지
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        log.info("Refresh Token 쿠키 추가: name={}, httpOnly={}, secure={}, maxAge={}초",
                jwtProperties.getCookie().getName(),
                jwtProperties.getCookie().isHttpOnly(),
                jwtProperties.getCookie().isSecure(),
                jwtProperties.getCookie().getMaxAge()
        );
    }
}
