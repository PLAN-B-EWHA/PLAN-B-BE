package com.planB.myexpressionfriend.common.constants;

public class JWTConstants {

    // JWT 토큰 만료 시간 (분 단위)
    public static final int ACCESS_TOKEN_EXPIRE_MINUTES = 30;      // 30분
    public static final int REFRESH_TOKEN_EXPIRE_MINUTES = 60 * 24 * 7;  // 7일

    // 쿠키 설정
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    public static final int REFRESH_TOKEN_COOKIE_MAX_AGE = 60 * 60 * 24 * 7;  // 7일 (초 단위)

    // 쿠키 경로
    public static final String COOKIE_PATH = "/";

    // 프로덕션 환경에서는 true로 설정 (HTTPS만 허용)
    public static final boolean COOKIE_SECURE = false;  // 개발: false, 프로덕션: true

    // SameSite 설정
    public static final String COOKIE_SAME_SITE = "Lax";  // Strict, Lax, None
}
