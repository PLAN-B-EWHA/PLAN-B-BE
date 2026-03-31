package myexpressionfriend_api.child.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.auth.domain.user.UserRole;
import myexpressionfriend_api.auth.repository.UserRepository;
import myexpressionfriend_api.child.domain.ChildPermissionType;
import myexpressionfriend_api.child.domain.ChildrenAuthorizedUser;
import myexpressionfriend_api.child.domain.Child;
import myexpressionfriend_api.child.dto.AuthorizedUserResponseDTO;
import myexpressionfriend_api.child.dto.ChildAuthorizationCreateDTO;
import myexpressionfriend_api.child.dto.ChildAuthorizationUpdateDTO;
import myexpressionfriend_api.child.repository.ChildRepository;
import myexpressionfriend_api.child.repository.ChildrenAuthorizedUserRepository;
import myexpressionfriend_api.common.exception.ConflictException;
import myexpressionfriend_api.common.exception.EntityNotFoundException;
import myexpressionfriend_api.common.exception.InvalidRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * ChildAuthorization Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChildAuthorizationService {

    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final ChildrenAuthorizedUserRepository authorizedUserRepository;

    /**
     * 권한 부여 (주보호자만 가능)
     * - isPrimary는 transferPrimaryParent API로만 변경 가능
     */
    @Transactional
    public AuthorizedUserResponseDTO grantAuthorization(
            UUID childId, UUID grantorUserId, ChildAuthorizationCreateDTO createDTO
    ) {
        log.info("권한 부여 - childId: {}, grantorUserId: {}, targetUserId: {}",
                childId, grantorUserId, createDTO.getUserId());

        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));

        if (!child.isPrimaryParent(grantorUserId)) {
            throw new InvalidRequestException("주 보호자만 권한을 부여할 수 있습니다.");
        }

        User targetUser = userRepository.findById(createDTO.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("대상 사용자를 찾을 수 없습니다."));

        // grantor는 이미 로드된 authorizedUsers에서 꺼냄 (중복 DB 조회 방지)
        User grantor = child.getPrimaryParent()
                .orElseThrow(() -> new EntityNotFoundException("권한 부여자를 찾을 수 없습니다."));

        // 기존 레코드 확인 (활성/비활성 모두)
        ChildrenAuthorizedUser authorization = authorizedUserRepository
                .findByChildAndUser(child, targetUser)
                .map(existing -> {
                    if (Boolean.TRUE.equals(existing.getIsActive())) {
                        throw new ConflictException("이미 권한이 부여된 사용자입니다.");
                    }
                    // 비활성 레코드 재활성화 (권한 목록 갱신 포함)
                    existing.clearPermissions();
                    createDTO.getPermissions().forEach(existing::addPermission);
                    existing.activate();
                    return existing;
                })
                .orElseGet(() -> {
                    // 레코드 없으면 신규 생성
                    ChildrenAuthorizedUser newAuth = ChildrenAuthorizedUser.builder()
                            .child(child)
                            .user(targetUser)
                            .isPrimary(false)
                            .permissions(createDTO.getPermissions())
                            .authorizedBy(grantor)
                            .isActive(true)
                            .build();
                    child.addAuthorizedUser(newAuth);
                    return authorizedUserRepository.save(newAuth);
                });

        log.info("권한 부여 완료 - childId: {}, targetUserId: {}", childId, createDTO.getUserId());

        return AuthorizedUserResponseDTO.from(authorization);
    }

    /**
     * 권한 수정 (주보호자만 가능)
     * - 주보호자 본인 권한은 수정 불가
     */
    @Transactional
    public AuthorizedUserResponseDTO updateAuthorization(
            UUID childId,
            UUID grantorUserId,
            UUID targetUserId,
            ChildAuthorizationUpdateDTO updateDTO
    ) {
        log.info("권한 수정 - childId: {}, grantorUserId: {}, targetUserId: {}",
                childId, grantorUserId, targetUserId);

        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));

        if (!child.isPrimaryParent(grantorUserId)) {
            throw new InvalidRequestException("주 보호자만 권한을 수정할 수 있습니다.");
        }

        ChildrenAuthorizedUser authorization = authorizedUserRepository
                .findByChildIdAndUserId(childId, targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("권한 정보를 찾을 수 없습니다."));

        if (Boolean.TRUE.equals(authorization.getIsPrimary())) {
            throw new InvalidRequestException("주 보호자 권한은 수정할 수 없습니다.");
        }

        if (updateDTO.getPermissions() != null) {
            authorization.clearPermissions();
            updateDTO.getPermissions().forEach(authorization::addPermission);
        }

        if (updateDTO.getIsActive() != null) {
            if (updateDTO.getIsActive()) {
                authorization.activate();
            } else {
                authorization.deactivate();
            }
        }

        log.info("권한 수정 완료 - childId: {}, targetUserId: {}", childId, targetUserId);

        return AuthorizedUserResponseDTO.from(authorization);
    }

    /**
     * 권한 해제 (주보호자만 가능)
     * - 주보호자 본인 권한은 해제 불가
     */
    @Transactional
    public void revokeAuthorization(UUID childId, UUID grantorUserId, UUID targetUserId) {
        log.info("권한 해제 - childId: {}, grantorUserId: {}, targetUserId: {}",
                childId, grantorUserId, targetUserId);

        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));

        if (!child.isPrimaryParent(grantorUserId)) {
            throw new InvalidRequestException("주 보호자만 권한을 해제할 수 있습니다.");
        }

        ChildrenAuthorizedUser authorization = authorizedUserRepository
                .findByChildIdAndUserId(childId, targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("권한 정보를 찾을 수 없습니다."));

        if (Boolean.TRUE.equals(authorization.getIsPrimary())) {
            throw new InvalidRequestException("주 보호자 권한은 해제할 수 없습니다.");
        }

        authorization.deactivate();

        log.info("권한 해제 완료 - childId: {}, targetUserId: {}", childId, targetUserId);
    }

    /**
     * 권한 목록 조회 (접근 가능한 사용자만)
     */
    public List<AuthorizedUserResponseDTO> getAuthorizedUsers(UUID childId, UUID requestUserId) {
        log.info("권한 목록 조회 - childId: {}, requestUserId: {}", childId, requestUserId);

        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new EntityNotFoundException("아동을 찾을 수 없습니다."));

        if (!child.canAccess(requestUserId)) {
            throw new InvalidRequestException("해당 아동에 대한 접근 권한이 없습니다.");
        }

        return authorizedUserRepository.findActiveByChildId(childId).stream()
                .map(AuthorizedUserResponseDTO::from)
                .toList();
    }

    /**
     * 내가 주보호자인 아동 ID 목록 조회
     */
    public List<UUID> getMyPrimaryChildrenIds(UUID userId) {
        log.info("내 주 보호자 아동 목록 조회 - userId: {}", userId);

        return authorizedUserRepository.findPrimaryByUserId(userId).stream()
                .map(au -> au.getChild().getChildId())
                .toList();
    }

    /**
     * 특정 권한 보유 여부 확인
     */
    public boolean hasPermission(UUID childId, UUID userId, ChildPermissionType permission) {
        return authorizedUserRepository.existsByChildIdAndUserIdAndPermission(
                childId, userId, permission
        );
    }
}
