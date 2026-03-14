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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class JWTCheckFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        return path.startsWith("/api/auth/")
                || path.startsWith("/api/public/")
                || path.equals("/api/unity/missions")           // Unity 런타임 미션 조회 (공개)
                || path.equals("/api/unity/missions/latest")    // 검증용 최근 미션 조회 (공개)
                || path.equals("/api/unity/game-results")       // 게임 결과 저장 (sessionToken으로 인증)
                || path.startsWith("/api/game/")
                || path.startsWith("/actuator/")
                || path.startsWith("/uploads/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/api-docs")
                || path.equals("/error")
                || path.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            String authHeader = request.getHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("GameSession ")) {
                filterChain.doFilter(request, response);
                return;
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new CustomJWTException("MissingToken");
            }

            String token = authHeader.substring(7);
            Map<String, Object> claims = jwtUtil.validateToken(token);

            String tokenType = (String) claims.get("type");
            if (!"access".equals(tokenType)) {
                throw new CustomJWTException("InvalidTokenType");
            }

            String userId = (String) claims.get("userId");
            String email = (String) claims.get("email");
            String name = (String) claims.get("name");
            @SuppressWarnings("unchecked")
            List<String> roleNames = (List<String>) claims.get("roles");

            if (roleNames.stream().anyMatch("PENDING"::equalsIgnoreCase)) {
                throw new CustomJWTException("PendingAccount");
            }

            List<SimpleGrantedAuthority> authorities = roleNames.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());

            UserDTO userDTO = UserDTO.builder()
                    .userId(UUID.fromString(userId))
                    .email(email)
                    .name(name)
                    .build();

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(userDTO, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            filterChain.doFilter(request, response);
        } catch (CustomJWTException e) {
            log.warn("JWT authentication failed: {}", e.getMessage());
            handleJWTException(response, e);
        } catch (Exception e) {
            log.error("JWT filter error: {}", e.getMessage(), e);
            handleJWTException(response, new CustomJWTException("Error"));
        }
    }

    private void handleJWTException(HttpServletResponse response, CustomJWTException e)
            throws IOException {
        String errorCode = e.getMessage();
        int statusCode;
        String message;

        switch (errorCode) {
            case "MissingToken":
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;
                message = "Authentication token is required";
                break;
            case "MalFormed":
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;
                message = "Malformed token";
                break;
            case "Expired":
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;
                message = "Token expired";
                break;
            case "Invalid":
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;
                message = "Invalid token";
                break;
            case "InvalidTokenType":
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;
                message = "Invalid token type";
                break;
            case "PendingAccount":
                statusCode = HttpServletResponse.SC_FORBIDDEN;
                message = "Account approval is required";
                break;
            default:
                statusCode = HttpServletResponse.SC_UNAUTHORIZED;
                message = "Authentication failed";
        }

        Map<String, Object> errorResponse = Map.of(
                "success", false,
                "error", "ERROR_ACCESS_TOKEN",
                "code", errorCode,
                "message", message,
                "timestamp", System.currentTimeMillis()
        );

        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}

