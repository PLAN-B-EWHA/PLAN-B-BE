package com.planB.myexpressionfriend.common.controller;

import com.planB.myexpressionfriend.common.dto.common.ApiResponse;
import com.planB.myexpressionfriend.common.dto.user.UserLoginDTO;
import com.planB.myexpressionfriend.common.dto.user.UserRegisterDTO;
import com.planB.myexpressionfriend.common.dto.user.UserResponseDTO;
import com.planB.myexpressionfriend.common.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입
     * POST /api/auth/register
     *
     * @param registerDTO 회원가입 정보
     * @return 생성된 사용자 정보
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(
            @Valid @RequestBody UserRegisterDTO registerDTO
            ) {

        log.info("============= 회원가입 요청 =============");
        log.info("이메일: {}", registerDTO.getEmail());
        log.info("이름: {}", registerDTO.getName());

        UserResponseDTO userResponse = authService.register(registerDTO);

        log.info("회원가입 성공: {}", userResponse.getEmail());

        return ResponseEntity.ok(
                ApiResponse.success("회원가입이 완료되었습니다", userResponse)
        );
    }


    /**
     * 로그인
     * POST /api/auth/login
     *
     * 참고: 실제 로그인 처리는 Spring Security가 자동으로 처리합니다.
     * 이 메서드는 API 문서화 목적으로만 존재합니다.
     *
     * 성공 시: APILoginSuccessHandler 실행
     *   - Access Token을 JSON Body에 반환
     *   - Refresh Token을 HttpOnly 쿠키에 저장
     *
     * 실패 시: APILoginFailHandler 실행
     *
     * @param loginDTO 로그인 정보 (email, password)
     * @return 토큰 및 사용자 정보
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody UserLoginDTO loginDTO
    ) {
        // 이 메서드는 실제로 실행되지 않습니다.
        // Spring Security의 UsernamePasswordAuthenticationFilter가 가로챕니다.
        log.info("이 로그는 출력되지 않습니다 - Spring Security가 처리");

        return ResponseEntity.ok(
                ApiResponse.success("This endpoint is handled by Spring Security")
        );
    }

    /**
     * 토큰 갱신
     * POST /api/auth/refresh
     *
     * Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.
     * Refresh Token은 쿠키에서 자동으로 추출됩니다.
     *
     * @param authHeader Authorization 헤더 (Bearer {accessToken})
     * @param refreshToken 쿠키에서 추출한 Refresh Token
     * @param response HttpServletResponse (새 Refresh Token 쿠키 설정용)
     * @return 새로운 Access Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refresh(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        log.info("============= 토큰 갱신 요청 =============");

        Map<String, String> tokens = authService.refreshToken(authHeader, refreshToken);

        // 새 Refresh Token을 HttpOnly 쿠키에 저장
        String newRefreshToken = tokens.get("refreshToken");
        if (newRefreshToken != null) {
            addRefreshTokenCookie(response, newRefreshToken);
        }

        // Access Token만 응답에 포함 (Refresh Token은 쿠키에 있음)
        Map<String, String> responseData = Map.of(
                "accessToken", tokens.get("accessToken")
        );

        log.info("토큰 갱신 성공");

        return ResponseEntity.ok(
                ApiResponse.success("토큰이 갱신되었습니다", responseData)
        );
    }

    /**
     * 로그아웃
     * POST /api/auth/logout
     *
     * Refresh Token 쿠키를 삭제합니다.
     * Access Token은 클라이언트에서 삭제해야 합니다.
     *
     * @param response HttpServletResponse (쿠키 삭제용)
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        log.info("============= 로그아웃 요청 =============");

        // Refresh Token 쿠키 삭제
        deleteRefreshTokenCookie(response);

        log.info("로그아웃 성공");

        return ResponseEntity.ok(
                ApiResponse.success("로그아웃되었습니다")
        );
    }

    /**
     * 이메일 중복 확인
     * GET /api/auth/check-email?email={email}
     *
     * @param email 확인할 이메일
     * @return 사용 가능 여부
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkEmail(
            @RequestParam String email
    ) {
        log.info("============= 이메일 중복 확인 =============");
        log.info("이메일: {}", email);

        boolean available = authService.isEmailAvailable(email);

        Map<String, Boolean> result = Map.of("available", available);

        return ResponseEntity.ok(
                ApiResponse.success(
                        available ? "사용 가능한 이메일입니다" : "이미 사용 중인 이메일입니다",
                        result
                )
        );
    }

    // ============= Private Helper Methods =============

    /**
     * Refresh Token을 HttpOnly 쿠키에 추가
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)      // XSS 공격 방지
                .secure(false)       // 개발: false, 프로덕션: true (HTTPS)
                .path("/")
                .maxAge(60 * 60 * 24 * 7)  // 7일
                .sameSite("Lax")     // CSRF 공격 방지
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.debug("Refresh Token 쿠키 설정 완료");
    }

    /**
     * Refresh Token 쿠키 삭제
     */
    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)  // 즉시 만료
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.debug("Refresh Token 쿠키 삭제 완료");
    }
}
