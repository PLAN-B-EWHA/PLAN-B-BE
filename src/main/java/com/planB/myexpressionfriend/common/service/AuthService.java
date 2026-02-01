package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.config.JWTProperties;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.dto.user.UserRegisterDTO;
import com.planB.myexpressionfriend.common.dto.user.UserResponseDTO;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import com.planB.myexpressionfriend.common.util.CustomJWTException;
import com.planB.myexpressionfriend.common.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final JWTProperties jwtProperties;

    /**
     * 회원가입
     */
    public UserResponseDTO register(UserRegisterDTO registerDTO) {
        log.info("회원가입 처리 시작: {}", registerDTO.getEmail());

        // 이메일 중복 체크
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            log.error("이메일 중복: {}", registerDTO.getEmail());
            throw new RuntimeException("이미 존재하는 이메일입니다");
        }

        // 역할 설정 (기본값: PARENT)
        UserRole role = UserRole.PARENT;
        if (registerDTO.getRole() != null) {
            try {
                role = UserRole.valueOf(registerDTO.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("잘못된 역할 값, 기본값(USER) 사용: {}", registerDTO.getRole());
            }
        }

        // User 엔티티 생성
        User user = User.builder()
                .email(registerDTO.getEmail())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .name(registerDTO.getName())
                .roles(Set.of(role))
                .build();

        // 저장
        User savedUser = userRepository.save(user);

        log.info("회원가입 완료: {}", savedUser.getEmail());

        return UserResponseDTO.from(savedUser);
    }


    /**
     * 이메일 중복 확인
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * 토큰 갱신
     */
    public Map<String, String> refreshToken(String authHeader, String refreshToken) {
        log.info("토큰 갱신 처리 시작");

        // Refresh Token 확인
        if (refreshToken == null || refreshToken.isEmpty()) {
            log.error("Refresh Token이 없습니다");
            throw new CustomJWTException("MissingRefreshToken");
        }

        try {
            // Access Token 검증 (만료 여부 확인용)
            String accessToken = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
            }

            Map<String, Object> claims = null;

            // Access Token이 유효한 경우
            if (accessToken != null) {
                try {
                    claims = jwtUtil.validateToken(accessToken);
                    log.info("Access Token이 아직 유효함");

                    // 유효하면 그대로 반환
                    return Map.of(
                            "accessToken", accessToken,
                            "refreshToken", refreshToken
                    );
                } catch (CustomJWTException e) {
                    log.info("Access Token 만료 또는 무효: {}", e.getMessage());
                }
            }

            // Refresh Token 검증
            claims = jwtUtil.validateToken(refreshToken);
            log.info("Refresh Token 검증 성공");

            // 새로운 Access Token 생성
            String newAccessToken = jwtUtil.generateToken(
                    claims,
                    jwtProperties.getAccessTokenExpireMinutes()
            );

            // 새로운 Refresh Token 생성 (Rotation)
            String newRefreshToken = jwtUtil.generateToken(
                    claims,
                    jwtProperties.getRefreshTokenExpireMinutes()
            );

            log.info("새 Access Token 및 Refresh Token 발급 완료");

            return Map.of(
                    "accessToken", newAccessToken,
                    "refreshToken", newRefreshToken
            );

        } catch (CustomJWTException e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            throw e;
        }
    }
}
