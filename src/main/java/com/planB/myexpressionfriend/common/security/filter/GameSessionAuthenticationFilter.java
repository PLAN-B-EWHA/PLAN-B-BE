package com.planB.myexpressionfriend.common.security.filter;

import com.planB.myexpressionfriend.common.domain.game.GameSession;
import com.planB.myexpressionfriend.common.repository.GameSessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

/**
 * 게임 세션 토큰 인증 필터
 *
 * 게임 전용 API에서 세션 토큰으로 인증
 * 경로: /api/game/**, /api/unity/**
 */
@RequiredArgsConstructor
@Slf4j
public class GameSessionAuthenticationFilter extends OncePerRequestFilter {

    private final GameSessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 게임 API 경로만 처리
        String path = request.getRequestURI();
        if (!path.startsWith("/api/game/") && !path.startsWith("/api/unity/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization 헤더에서 세션 토큰 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("GameSession ")) {
            String sessionToken = authHeader.substring(12);

            // 세션 검증
            Optional<GameSession> sessionOpt = sessionRepository.findValidSessionByToken(
                    sessionToken, LocalDateTime.now()
            );

            if (sessionOpt.isPresent()) {
                GameSession session = sessionOpt.get();

                // 인증 객체 생성 (ROLE_GAME 권한 부여)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                session.getChild().getChildId(),
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_GAME"))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("게임 세션 인증 성공 - childId: {}", session.getChild().getChildId());
            }
        }

        filterChain.doFilter(request, response);
    }
}