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
@Tag(name = "Auth", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;
    private final JWTProperties jwtProperties;

    @PostMapping("/register")
    @Operation(summary = "Register", description = "Create a new account with email and password.")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(
            @Valid @RequestBody UserRegisterDTO registerDTO
    ) {
        UserResponseDTO userResponse = authService.register(registerDTO);
        return ResponseEntity.ok(ApiResponse.success("Registration completed.", userResponse));
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Issue access and refresh tokens.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody UserLoginDTO loginDTO
    ) {
        Map<String, Object> loginResult = authService.login(loginDTO);
        return ResponseEntity.ok(ApiResponse.success("Login successful.", loginResult));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Issue a new access token using refresh token.")
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
        return ResponseEntity.ok(ApiResponse.success("Token refreshed.", responseData));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate refresh token and clear cookie.")
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
        return ResponseEntity.ok(ApiResponse.success("Logged out."));
    }

    @GetMapping("/check-email")
    @Operation(summary = "Check email availability", description = "Check if email can be used for registration.")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(@RequestParam String email) {
        boolean available = authService.isEmailAvailable(email);
        Map<String, Boolean> result = Map.of("available", available);
        String message = available ? "Email is available." : "Email is already in use.";
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

