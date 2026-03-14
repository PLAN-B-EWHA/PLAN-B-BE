package com.planB.myexpressionfriend.common.config;

import com.planB.myexpressionfriend.common.dto.user.UserDTO;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA 감사(Auditing)에서 현재 작업자 UUID를 제공합니다.
 *
 * <p>@CreatedBy, @LastModifiedBy 필드에 자동으로 값을 주입합니다.
 * - JWT 인증: UserDTO → getUserId()
 * - 게임 세션 인증: principal이 UUID(childId)이므로 auditor로 사용하지 않음
 * - 미인증 요청(배치, 스케줄러 등): empty 반환</p>
 */
@Component("auditorAware")
public class AuditorAwareImpl implements AuditorAware<UUID> {

    @Override
    public Optional<UUID> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDTO userDTO) {
            return Optional.of(userDTO.getUserId());
        }

        // 게임 세션 인증(principal = UUID childId)은 사용자 auditor로 사용하지 않음
        return Optional.empty();
    }
}
