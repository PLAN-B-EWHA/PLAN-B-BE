package com.planB.myexpressionfriend.common.integration;

import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.stream.Collectors;

/**
 * 테스트용 Security Context 헬퍼
 */
public class TestSecurityConfig {

    /**
     * User 엔티티로 인증 컨텍스트 설정
     */
    public static void setAuthentication(User user) {
        UserDTO userDTO = UserDTO.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .roles(user.getRoles())
                .build();

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toList());

        var authentication = new UsernamePasswordAuthenticationToken(
                userDTO, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 인증 컨텍스트 초기화
     */
    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}