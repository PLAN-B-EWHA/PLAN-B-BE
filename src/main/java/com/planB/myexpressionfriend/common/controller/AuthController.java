package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.config.JWTProperties;
import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import com.planB.myexpressionfriend.common.dto.user.UserLoginDTO;
import com.planB.myexpressionfriend.common.dto.user.UserRegisterDTO;
import com.planB.myexpressionfriend.common.dto.user.UserResponseDTO;
import com.planB.myexpressionfriend.common.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 API")
public class AuthController {

    private final AuthService authService;
    private final JWTProperties jwtProperties;

    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 새 계정을 생성합니다.")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(
            @Valid @RequestBody UserRegisterDTO registerDTO
    ) {
        UserResponseDTO userResponse = authService.register(registerDTO);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", userResponse));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "액세스 토큰과 리프레시 토큰을 발급합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody UserLoginDTO loginDTO
    ) {
        Map<String, Object> loginResult = authService.login(loginDTO);
        return ResponseEntity.ok(ApiResponse.success("로그인에 성공했습니다.", loginResult));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 새 액세스 토큰을 발급합니다.")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        Map<String, String> tokens = authService.refreshToken(refreshToken);
        String newRefreshToken = tokens.get("refreshToken");

        if (newRefreshToken != null) {
            addRefreshTokenCookie(response, newRefreshToken);
        }

        Map<String, String> responseData = Map.of("accessToken", tokens.get("accessToken"));
        return ResponseEntity.ok(ApiResponse.success("토큰이 재발급되었습니다.", responseData));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "리프레시 토큰을 무효화하고 쿠키를 제거합니다.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserDTO currentUser,
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logoutByRefreshToken(refreshToken);
        } else if (currentUser != null && currentUser.getUserId() != null) {
            authService.logout(currentUser.getUserId().toString());
        }
        deleteRefreshTokenCookie(response);
        return ResponseEntity.ok(ApiResponse.success("로그아웃되었습니다."));
    }

    @GetMapping("/check-email")
    @Operation(summary = "이메일 사용 가능 여부 확인", description = "회원가입에 사용할 수 있는 이메일인지 확인합니다.")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(@RequestParam String email) {
        boolean available = authService.isEmailAvailable(email);
        Map<String, Boolean> result = Map.of("available", available);
        String message = available ? "사용 가능한 이메일입니다." : "이미 사용 중인 이메일입니다.";
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.getCookie().getName(), refreshToken)
                .httpOnly(jwtProperties.getCookie().isHttpOnly())
                .secure(jwtProperties.getCookie().isSecure())
                .path(jwtProperties.getCookie().getPath())
                .maxAge(jwtProperties.getCookie().getMaxAge())
                .sameSite(jwtProperties.getCookie().getSameSite())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(jwtProperties.getCookie().getName(), "")
                .httpOnly(jwtProperties.getCookie().isHttpOnly())
                .secure(jwtProperties.getCookie().isSecure())
                .path(jwtProperties.getCookie().getPath())
                .maxAge(0)
                .sameSite(jwtProperties.getCookie().getSameSite())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}