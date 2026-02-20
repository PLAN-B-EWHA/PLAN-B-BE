package com.planB.myexpressionfriend.common.util;

import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * 인증 컨텍스트에서 현재 사용자 식별자를 안전하게 추출하는 유틸.
 */
public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    public static UUID getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDTO)) {
            throw new IllegalStateException("지원하지 않는 인증 주체 타입입니다.");
        }

        return ((UserDTO) principal).getUserId();
    }
}
