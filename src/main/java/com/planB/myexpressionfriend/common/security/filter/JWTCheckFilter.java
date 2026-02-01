package com.planB.myexpressionfriend.common.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.util.CustomJWTException;
import com.planB.myexpressionfriend.common.util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class JWTCheckFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final ObjectMapper objectMapper;

    /**
     * 필터를 적용하지 않을 경로 설정
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {

        String path = request.getRequestURI();

        log.debug("요청 경로 확인: {}", path);

        // OPTIONS 요청은 필터 건너뛰기 (CORS preflight)
        if (request.getMethod().equals("OPTIONS")) {
            log.debug("OPTIONS 요청 - 필터 건너뛰기");
            return true;
        }

        // 인증 없이 접근 가능한 경로
        if (path.startsWith("/api/auth/") || path.startsWith("/api/public/")) {
            log.debug("인증 불필요 경로 - 필터 건너뛰기");
            return true;
        }

        log.debug("JWT 검증 필터 적용");
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        log.info("============= JWT 검증 필터 실행 =============");
        log.info("요청 URI: {}", request.getRequestURI());
        log.info("요청 Method: {}", request.getMethod());

        try {
            // Authorization 헤더에서 토큰 추출
            String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.error("Authorization 헤더가 없거나 형식이 잘못되었습니다");
                throw new CustomJWTException("MissingToken");
            }

            // "Bearer " 제거하고 토큰만 추출
            String token = authHeader.substring(7);
            log.debug("추출된 토큰: {}...", token.substring(0, Math.min(token.length(), 20)));

            // 토큰 검증 및 Claims 추출
            Map<String, Object> claims = jwtUtil.validateToken(token);

            log.info("토큰 검증 성공");
            log.info("사용자 이메일: {}", claims.get("email"));
            log.info("사용자 역할: {}", claims.get("roles"));

            // Claims에서 사용자 정보 추출
            String userId = (String) claims.get("userId");
            String email = (String) claims.get("email");
            String name = (String) claims.get("name");
            List<String> roleNames = (List<String>) claims.get("roles");

            // Spring Security 권한 객체 생성
            List<SimpleGrantedAuthority> authorities = roleNames.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            // Spring Security 인증 객체 생성
            UserDTO userDTO = UserDTO.builder()
                    .userId(UUID.fromString(userId))
                    .email(email)
                    .name(name)
                    .build();

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            userDTO,  // ← UserDTO 객체
                            null,
                            authorities
                    );

            // 추가 정보 저장 (선택사항)
            authenticationToken.setDetails(Map.of(
                    "userId", userId,
                    "name", name,
                    "email", email
            ));

            // SecurityContext에 인증 정보 설정
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            log.info("SecurityContext에 인증 정보 설정 완료");
            log.info("인증된 사용자: {}, 권한: {}", email, authorities);

            // 다음 필터로 진행
            filterChain.doFilter(request, response);
        } catch (CustomJWTException e) {
            log.error("JWT 검증 실패: {}", e.getMessage());
            handleJWTException(response, e);
        } catch (Exception e) {
            log.error("필터 처리 중 예외 발생: {}", e.getMessage(), e);
            handleJWTException(response, new CustomJWTException("Error"));
        }
    }

    /**
     * JWT 예외 처리 - JSON 에러 응답
     */
    private void handleJWTException(HttpServletResponse response, CustomJWTException e)
            throws IOException {

        log.error("JWT 에러 응답 전송: {}", e.getMessage());

        // 에러 코드별 상태 코드 및 메시지 설정
        String errorCode = e.getMessage();
        int statusCode;
        String message;

        switch (errorCode) {
            case "MissingToken":
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;  // 401
                message = "인증 토큰이 필요합니다";
                break;
            case "MalFormed":
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;  // 401
                message = "토큰 형식이 올바르지 않습니다";
                break;
            case "Expired":
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;  // 401
                message = "토큰이 만료되었습니다";
                break;
            case "Invalid":
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;  // 401
                message = "유효하지 않은 토큰입니다";
                break;
            default:
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;  // 401
                message = "인증에 실패했습니다";
        }

        // JSON 에러 응답 생성
        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", "ERROR_ACCESS_TOKEN",
                "code", errorCode,
                "message", message,
                "timestamp", System.currentTimeMillis()
        );

        // 응답 설정
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // JSON 응답 전송
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
