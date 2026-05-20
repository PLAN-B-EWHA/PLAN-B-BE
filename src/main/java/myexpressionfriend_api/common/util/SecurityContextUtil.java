package myexpressionfriend_api.common.util;

import myexpressionfriend_api.auth.dto.UserDTO;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/**
 * Utility for safely reading the current authenticated user id.
 */
public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    public static UUID getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Authentication is missing.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDTO userDTO) {
            return userDTO.getUserId();
        }

        throw new IllegalStateException("Unsupported authentication principal type: " + principal.getClass().getSimpleName());
    }
}