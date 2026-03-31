package myexpressionfriend_api.auth.service;

import io.jsonwebtoken.Claims;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @InjectMocks AuthService authService;

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JWTUtil jwtUtil;
    @Mock JWTProperties jwtProperties;

    private final UUID userId = UUID.randomUUID();
    private final String email = "test@example.com";
    private final String rawPassword = "password123";
    private final String encodedPassword = "encoded_password";

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(userId)          // getUserId() null 방지
                .email(email)
                .password(encodedPassword)
                .name("테스터")
                .roles(Set.of(UserRole.PARENT))
                .build();
    }

    // ============= register =============

    @Nested
    @DisplayName("회원가입")
    class Register {

        @Test
        @DisplayName("새 이메일로 회원가입하면 PENDING 역할의 유저가 생성된다")
        void register_success() {
            UserRegisterDTO dto = UserRegisterDTO.builder()
                    .email(email).password(rawPassword).name("테스터").build();

            given(userRepository.existsByEmail(email)).willReturn(false);
            given(passwordEncoder.encode(rawPassword)).willReturn(encodedPassword);
            given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

            UserResponseDTO result = authService.register(dto);

            assertThat(result.getEmail()).isEqualTo(email);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("중복 이메일이면 ConflictException이 발생한다")
        void register_duplicateEmail_throwsConflict() {
            UserRegisterDTO dto = UserRegisterDTO.builder()
                    .email(email).password(rawPassword).name("테스터").build();

            given(userRepository.existsByEmail(email)).willReturn(true);

            assertThatThrownBy(() -> authService.register(dto))
                    .isInstanceOf(ConflictException.class);
            verify(userRepository, never()).save(any());
        }
    }

    // ============= login =============

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("올바른 자격증명이면 accessToken과 refreshToken을 반환한다")
        void login_success() {
            UserLoginDTO dto = new UserLoginDTO(email, rawPassword);

            given(jwtProperties.getAccessTokenExpireMinutes()).willReturn(60);
            given(jwtProperties.getRefreshTokenExpireMinutes()).willReturn(10080);
            given(jwtUtil.generateToken(any(), anyInt())).willReturn("mock.jwt.token");
            given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(rawPassword, encodedPassword)).willReturn(true);
            given(refreshTokenRepository.findByUserId(any(UUID.class))).willReturn(Optional.empty());

            Map<String, Object> result = authService.login(dto);

            assertThat(result).containsKeys("accessToken", "refreshToken", "grantType", "expiresIn");
            assertThat(result.get("grantType")).isEqualTo("Bearer");
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 AuthenticationFailedException이 발생한다")
        void login_userNotFound_throwsAuthFailed() {
            UserLoginDTO dto = new UserLoginDTO(email, rawPassword);

            given(userRepository.findByEmail(email)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(dto))
                    .isInstanceOf(AuthenticationFailedException.class);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 AuthenticationFailedException이 발생한다")
        void login_wrongPassword_throwsAuthFailed() {
            UserLoginDTO dto = new UserLoginDTO(email, "wrongPassword");

            given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrongPassword", encodedPassword)).willReturn(false);

            assertThatThrownBy(() -> authService.login(dto))
                    .isInstanceOf(AuthenticationFailedException.class);
        }

        @Test
        @DisplayName("이미 리프레시 토큰이 존재하면 updateToken으로 갱신한다")
        void login_existingRefreshToken_updatesToken() {
            UserLoginDTO dto = new UserLoginDTO(email, rawPassword);
            RefreshToken existingToken = RefreshToken.builder()
                    .userId(userId)
                    .token("old.token")
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .build();

            given(jwtProperties.getAccessTokenExpireMinutes()).willReturn(60);
            given(jwtProperties.getRefreshTokenExpireMinutes()).willReturn(10080);
            given(jwtUtil.generateToken(any(), anyInt())).willReturn("mock.jwt.token");
            given(userRepository.findByEmail(email)).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches(rawPassword, encodedPassword)).willReturn(true);
            given(refreshTokenRepository.findByUserId(any())).willReturn(Optional.of(existingToken));

            authService.login(dto);

            verify(refreshTokenRepository).save(existingToken);
        }
    }

    // ============= isEmailAvailable =============

    @Nested
    @DisplayName("이메일 사용 가능 여부")
    class EmailAvailable {

        @Test
        @DisplayName("미사용 이메일이면 true를 반환한다")
        void isEmailAvailable_free_returnsTrue() {
            given(userRepository.existsByEmail(email)).willReturn(false);
            assertThat(authService.isEmailAvailable(email)).isTrue();
        }

        @Test
        @DisplayName("사용 중인 이메일이면 false를 반환한다")
        void isEmailAvailable_taken_returnsFalse() {
            given(userRepository.existsByEmail(email)).willReturn(true);
            assertThat(authService.isEmailAvailable(email)).isFalse();
        }
    }

    // ============= refreshToken =============

    @Nested
    @DisplayName("토큰 재발급")
    class TokenRefresh {

        @Test
        @DisplayName("null 토큰이면 MissingRefreshToken 예외가 발생한다")
        void refreshToken_null_throwsMissing() {
            assertThatThrownBy(() -> authService.refreshToken(null))
                    .isInstanceOf(CustomJWTException.class)
                    .hasMessage("MissingRefreshToken");
        }

        @Test
        @DisplayName("빈 문자열이면 MissingRefreshToken 예외가 발생한다")
        void refreshToken_empty_throwsMissing() {
            assertThatThrownBy(() -> authService.refreshToken(""))
                    .isInstanceOf(CustomJWTException.class)
                    .hasMessage("MissingRefreshToken");
        }

        @Test
        @DisplayName("type이 refresh가 아니면 InvalidTokenType 예외가 발생한다")
        void refreshToken_wrongType_throwsInvalidTokenType() {
            Claims claims = mock(Claims.class);
            given(claims.get("type")).willReturn("access");
            given(jwtUtil.validateToken("some.token")).willReturn(claims);

            assertThatThrownBy(() -> authService.refreshToken("some.token"))
                    .isInstanceOf(CustomJWTException.class)
                    .hasMessage("InvalidTokenType");
        }

        @Test
        @DisplayName("DB에 저장된 토큰이 없으면 InvalidRefreshToken 예외가 발생한다")
        void refreshToken_notFoundInDB_throwsInvalid() {
            Claims claims = mock(Claims.class);
            given(claims.get("type")).willReturn("refresh");
            given(claims.get("userId")).willReturn(userId.toString());
            given(jwtUtil.validateToken("some.token")).willReturn(claims);
            given(refreshTokenRepository.findByUserId(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refreshToken("some.token"))
                    .isInstanceOf(CustomJWTException.class)
                    .hasMessage("InvalidRefreshToken");
        }

        @Test
        @DisplayName("저장된 토큰과 불일치하면 InvalidRefreshToken 예외가 발생한다")
        void refreshToken_tokenMismatch_throwsInvalid() {
            Claims claims = mock(Claims.class);
            given(claims.get("type")).willReturn("refresh");
            given(claims.get("userId")).willReturn(userId.toString());
            given(jwtUtil.validateToken("incoming.token")).willReturn(claims);

            RefreshToken stored = RefreshToken.builder()
                    .userId(userId).token("different.token")
                    .expiresAt(LocalDateTime.now().plusDays(7)).build();
            given(refreshTokenRepository.findByUserId(userId)).willReturn(Optional.of(stored));

            assertThatThrownBy(() -> authService.refreshToken("incoming.token"))
                    .isInstanceOf(CustomJWTException.class)
                    .hasMessage("InvalidRefreshToken");
        }

        @Test
        @DisplayName("만료된 토큰이면 Expired 예외가 발생한다")
        void refreshToken_expired_throwsExpired() {
            Claims claims = mock(Claims.class);
            given(claims.get("type")).willReturn("refresh");
            given(claims.get("userId")).willReturn(userId.toString());
            given(jwtUtil.validateToken("expired.token")).willReturn(claims);

            RefreshToken stored = RefreshToken.builder()
                    .userId(userId).token("expired.token")
                    .expiresAt(LocalDateTime.now().minusMinutes(1)).build(); // 이미 만료
            given(refreshTokenRepository.findByUserId(userId)).willReturn(Optional.of(stored));

            assertThatThrownBy(() -> authService.refreshToken("expired.token"))
                    .isInstanceOf(CustomJWTException.class)
                    .hasMessage("Expired");
        }

        @Test
        @DisplayName("유효한 리프레시 토큰이면 새 액세스/리프레시 토큰을 반환한다")
        void refreshToken_success() {
            Claims claims = mock(Claims.class);
            given(claims.get("type")).willReturn("refresh");
            given(claims.get("userId")).willReturn(userId.toString());
            given(jwtUtil.validateToken("valid.refresh.token")).willReturn(claims);

            RefreshToken stored = RefreshToken.builder()
                    .userId(userId).token("valid.refresh.token")
                    .expiresAt(LocalDateTime.now().plusDays(7)).build();
            given(refreshTokenRepository.findByUserId(userId)).willReturn(Optional.of(stored));
            given(userRepository.findById(userId)).willReturn(Optional.of(testUser));
            given(jwtProperties.getAccessTokenExpireMinutes()).willReturn(60);
            given(jwtProperties.getRefreshTokenExpireMinutes()).willReturn(10080);
            given(jwtUtil.generateToken(any(), anyInt())).willReturn("new.token");

            Map<String, String> result = authService.refreshToken("valid.refresh.token");

            assertThat(result).containsKeys("accessToken", "refreshToken");
            verify(refreshTokenRepository).save(stored);
        }
    }
}
