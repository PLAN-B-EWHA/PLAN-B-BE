package myexpressionfriend_api.auth.service;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.auth.domain.token.RefreshToken;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.auth.domain.user.UserRole;
import myexpressionfriend_api.auth.dto.UserLoginDTO;
import myexpressionfriend_api.auth.dto.UserRegisterDTO;
import myexpressionfriend_api.auth.dto.UserResponseDTO;
import myexpressionfriend_api.auth.repository.RefreshTokenRepository;
import myexpressionfriend_api.auth.repository.UserRepository;
import myexpressionfriend_api.common.config.JWTProperties;
import myexpressionfriend_api.common.exception.AuthenticationFailedException;
import myexpressionfriend_api.common.exception.ConflictException;
import myexpressionfriend_api.security.util.CustomJWTException;
import myexpressionfriend_api.security.util.JWTUtil;
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
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }

        if (registerDTO.getRole() != null) {
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

        UUID userId = user.getUserId();
        RefreshToken refreshToken = refreshTokenRepository.findByUserId(userId)
                .orElse(null);

        LocalDateTime newExpiresAt = LocalDateTime.now().plusMinutes(jwtProperties.getRefreshTokenExpireMinutes());

        if (refreshToken == null) {
            refreshToken = RefreshToken.builder()
                    .userId(userId)
                    .token(refreshTokenValue)
                    .expiresAt(newExpiresAt)
                    .build();
        } else {
            refreshToken.updateToken(refreshTokenValue, newExpiresAt);
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
        refreshTokenRepository.deleteByUserId(UUID.fromString(userId));
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
            Claims claims = jwtUtil.validateToken(refreshTokenValue);
            String tokenType = (String) claims.get("type");
            if (!"refresh".equals(tokenType)) {
                return;
            }

            String userId = (String) claims.get("userId");
            if (userId != null && !userId.isBlank()) {
                refreshTokenRepository.deleteByUserId(UUID.fromString(userId));
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
            Claims refreshClaims = jwtUtil.validateToken(refreshTokenValue);

            String tokenType = (String) refreshClaims.get("type");
            if (!"refresh".equals(tokenType)) {
                throw new CustomJWTException("InvalidTokenType");
            }

            String userIdStr = (String) refreshClaims.get("userId");
            UUID userId = UUID.fromString(userIdStr);

            RefreshToken storedToken = refreshTokenRepository.findByUserId(userId)
                    .orElseThrow(() -> new CustomJWTException("InvalidRefreshToken"));

            if (!storedToken.matchesToken(refreshTokenValue)) {
                throw new CustomJWTException("InvalidRefreshToken");
            }

            if (storedToken.isExpired(LocalDateTime.now())) {
                throw new CustomJWTException("Expired");
            }

            User user = userRepository.findById(userId)
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

            LocalDateTime newExpiresAt = LocalDateTime.now().plusMinutes(jwtProperties.getRefreshTokenExpireMinutes());
            storedToken.updateToken(newRefreshTokenValue, newExpiresAt);
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
