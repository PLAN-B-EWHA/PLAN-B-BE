package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.config.JWTProperties;
import com.planB.myexpressionfriend.common.exception.AuthenticationFailedException;
import com.planB.myexpressionfriend.common.domain.token.RefreshToken;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.dto.user.UserLoginDTO;
import com.planB.myexpressionfriend.common.dto.user.UserRegisterDTO;
import com.planB.myexpressionfriend.common.dto.user.UserResponseDTO;
import com.planB.myexpressionfriend.common.repository.RefreshTokenRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import com.planB.myexpressionfriend.common.util.CustomJWTException;
import com.planB.myexpressionfriend.common.util.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;
    private final JWTProperties jwtProperties;

    public UserResponseDTO register(UserRegisterDTO registerDTO) {
        log.info("Register request: {}", registerDTO.getEmail());

        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (registerDTO.getRole() != null && !registerDTO.getRole().isBlank()) {
            log.warn("Requested role '{}' ignored. New users are created as PENDING", registerDTO.getRole());
        }

        User user = User.builder()
                .email(registerDTO.getEmail())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .name(registerDTO.getName())
                .roles(Set.of(UserRole.PENDING))
                .build();

        User savedUser = userRepository.save(user);
        return UserResponseDTO.from(savedUser);
    }

    public Map<String, Object> login(UserLoginDTO loginDTO) {
        log.info("Login request: {}", loginDTO.getEmail());

        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        Map<String, Object> accessClaims = Map.of(
                "type", "access",
                "userId", user.getUserId().toString(),
                "email", user.getEmail(),
                "name", user.getName(),
                "roles", user.getRoles()
        );

        String accessToken = jwtUtil.generateToken(
                accessClaims,
                jwtProperties.getAccessTokenExpireMinutes()
        );

        Map<String, Object> refreshClaims = Map.of(
                "type", "refresh",
                "userId", user.getUserId().toString(),
                "email", user.getEmail()
        );

        String refreshTokenValue = jwtUtil.generateToken(
                refreshClaims,
                jwtProperties.getRefreshTokenExpireMinutes()
        );

        String userId = user.getUserId().toString();
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(userId)
                .orElse(null);

        if (refreshToken == null) {
            refreshToken = RefreshToken.builder()
                    .userId(userId)
                    .token(refreshTokenValue)
                    .expiresAt(LocalDateTime.now().plusMinutes(jwtProperties.getRefreshTokenExpireMinutes()))
                    .build();
        } else {
            refreshToken.updateToken(refreshTokenValue, jwtProperties.getRefreshTokenExpireMinutes());
        }

        refreshTokenRepository.save(refreshToken);

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshTokenValue,
                "grantType", "Bearer",
                "expiresIn", jwtProperties.getAccessTokenExpireMinutes() * 60 * 1000L
        );
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    public void logout(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    /**
     * Server-side logout using refresh token cookie value.
     * - If token is invalid/expired, this method does not fail hard to keep logout idempotent.
     */
    public void logoutByRefreshToken(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }

        try {
            Map<String, Object> claims = jwtUtil.validateToken(refreshTokenValue);
            String tokenType = (String) claims.get("type");
            if (!"refresh".equals(tokenType)) {
                return;
            }

            String userId = (String) claims.get("userId");
            if (userId != null && !userId.isBlank()) {
                refreshTokenRepository.deleteByUserId(userId);
            }
        } catch (Exception ignored) {
            // Keep logout idempotent and avoid leaking token validation details.
        }
    }

    public Map<String, String> refreshToken(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isEmpty()) {
            throw new CustomJWTException("MissingRefreshToken");
        }

        try {
            Map<String, Object> refreshClaims = jwtUtil.validateToken(refreshTokenValue);

            String tokenType = (String) refreshClaims.get("type");
            if (!"refresh".equals(tokenType)) {
                throw new CustomJWTException("InvalidTokenType");
            }

            String userId = (String) refreshClaims.get("userId");

            RefreshToken storedToken = refreshTokenRepository.findByUserId(userId)
                    .orElseThrow(() -> new CustomJWTException("InvalidRefreshToken"));

            if (!storedToken.matchesToken(refreshTokenValue)) {
                throw new CustomJWTException("InvalidRefreshToken");
            }

            if (storedToken.isExpired()) {
                throw new CustomJWTException("Expired");
            }

            User user = userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new CustomJWTException("UserNotFound"));

            Map<String, Object> newAccessClaims = Map.of(
                    "type", "access",
                    "userId", user.getUserId().toString(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "roles", user.getRoles()
            );

            String newAccessToken = jwtUtil.generateToken(
                    newAccessClaims,
                    jwtProperties.getAccessTokenExpireMinutes()
            );

            Map<String, Object> newRefreshClaims = Map.of(
                    "type", "refresh",
                    "userId", user.getUserId().toString(),
                    "email", user.getEmail()
            );

            String newRefreshTokenValue = jwtUtil.generateToken(
                    newRefreshClaims,
                    jwtProperties.getRefreshTokenExpireMinutes()
            );

            storedToken.updateToken(newRefreshTokenValue, jwtProperties.getRefreshTokenExpireMinutes());
            refreshTokenRepository.save(storedToken);

            return Map.of(
                    "accessToken", newAccessToken,
                    "refreshToken", newRefreshTokenValue
            );

        } catch (CustomJWTException e) {
            throw e;
        }
    }
}
