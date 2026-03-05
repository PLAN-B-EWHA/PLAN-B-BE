package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.dto.child.AuthorizedUserDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildAuthorizationDTO;
import com.planB.myexpressionfriend.common.repository.ChildRepository;
import com.planB.myexpressionfriend.common.repository.ChildrenAuthorizedUserRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChildAuthorizationService {

    private final ChildRepository childRepository;
    private final UserRepository userRepository;
    private final ChildrenAuthorizedUserRepository  authorizedUserRepository;

    /**
     */
    @Transactional
    public AuthorizedUserDTO grantAuthorization(
            UUID childId, UUID grantorUserId, ChildAuthorizationDTO authorizationDTO
    ) {

        log.info("권한 부여 - childId: {}, grantorUserId: {}, targetUserId: {}",
                childId, grantorUserId, authorizationDTO.getUserId());

        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.isPrimaryParent(grantorUserId)) {
            throw new IllegalStateException("주 보호자만 권한을 부여할 수 있습니다.");
        }

        User targetUser = userRepository.findById(authorizationDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다."));

        boolean alreadyExists = authorizedUserRepository.existsByChildAndUser(child, targetUser);
        if (alreadyExists) {
            throw new IllegalStateException("이미 권한이 부여된 사용자입니다.");
        }

        boolean isPrimary = Boolean.TRUE.equals(authorizationDTO.getIsPrimary());
        if (isPrimary) {
            if (!targetUser.hasRole(UserRole.PARENT)) {
                throw new IllegalStateException("주 보호자는 PARENT 역할이어야 합니다.");
            }

            long primaryCount = authorizedUserRepository.countPrimaryByChildId(childId);
            if (primaryCount > 0) {
                throw new IllegalStateException("주 보호자는 1명만 지정할 수 있습니다.");
            }
        }

        User grantor = userRepository.findById(grantorUserId)
                .orElseThrow(() -> new IllegalArgumentException("권한 부여자를 찾을 수 없습니다."));

        ChildrenAuthorizedUser authorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(targetUser)
                .isPrimary(isPrimary)
                .permissions(authorizationDTO.getPermissions())
                .authorizedBy(grantor)
                .isActive(true)
                .build();

        child.addAuthorizedUser(authorization);
        authorizedUserRepository.save(authorization);

        log.info("권한 부여 완료 - childId: {}, targetUserId: {}, isPrimary: {}",
                childId, authorizationDTO.getUserId(), isPrimary);

        return AuthorizedUserDTO.from(authorization);
    }

    /**
     */
    @Transactional
    public AuthorizedUserDTO updateAuthorization(
            UUID childId,
            UUID grantorUserId,
            UUID targetUserId,
            ChildAuthorizationDTO authorizationDTO
    ) {
        log.info("권한 수정 - childId: {}, grantorUserId: {}, targetUserId: {}",
                childId, grantorUserId, targetUserId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.isPrimaryParent(grantorUserId)) {
            throw new IllegalStateException("주 보호자만 권한을 수정할 수 있습니다.");
        }

        ChildrenAuthorizedUser authorization = authorizedUserRepository
                .findByChildIdAndUserId(childId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("권한 정보를 찾을 수 없습니다."));

        if (Boolean.TRUE.equals(authorization.getIsPrimary())) {
            throw new IllegalStateException("주 보호자 권한은 수정할 수 없습니다.");
        }

        authorization.clearPermissions();
        authorizationDTO.getPermissions().forEach(authorization::addPermission);

        log.info("권한 수정 완료 - childId: {}, targetUserId: {}", childId, targetUserId);

        return AuthorizedUserDTO.from(authorization);
    }

    /**
     */
    @Transactional
    public void revokeAuthorization(UUID childId, UUID grantorUserId, UUID targetUserId) {
        log.info("권한 해제 - childId: {}, grantorUserId: {}, targetUserId: {}",
                childId, grantorUserId, targetUserId);

        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.isPrimaryParent(grantorUserId)) {
            throw new IllegalStateException("주 보호자만 권한을 해제할 수 있습니다.");
        }

        ChildrenAuthorizedUser authorization = authorizedUserRepository
                .findByChildIdAndUserId(childId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("권한 정보를 찾을 수 없습니다."));

        if (Boolean.TRUE.equals(authorization.getIsPrimary())) {
            throw new IllegalStateException("주 보호자 권한은 해제할 수 없습니다.");
        }

        authorization.deactivate();

        log.info("권한 해제 완료 - childId: {}, targetUserId: {}", childId, targetUserId);
    }

    /**
     */
    public List<AuthorizedUserDTO> getAuthorizedUsers(UUID childId, UUID requestUserId) {
        log.info("권한 목록 조회 - childId: {}, requestUserId: {}", childId, requestUserId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.canAccess(requestUserId)) {
            throw new IllegalStateException("해당 아동에 대한 접근 권한이 없습니다.");
        }

        List<ChildrenAuthorizedUser> authorizations = authorizedUserRepository
                .findActiveByChildId(childId);

        return authorizations.stream()
                .map(AuthorizedUserDTO::from)
                .collect(Collectors.toList());
    }

    /**
     */
    public List<UUID> getMyPrimaryChildrenIds(UUID userId) {
        log.info("내 주 보호자 아동 목록 조회 - userId: {}", userId);

        List<ChildrenAuthorizedUser> authorizations = authorizedUserRepository
                .findPrimaryByUserId(userId);

        return authorizations.stream()
                .map(au -> au.getChild().getChildId())
                .collect(Collectors.toList());
    }

    /**
     */
    public boolean hasPermission(UUID childId, UUID userId, ChildPermissionType permission) {
        return authorizedUserRepository.existsByChildIdAndUserIdAndPermission(
                childId, userId, permission
        );
    }
}
