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
 * 아동 권한 관리 서비스
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
     * 권한 부여 (주보호자만 가능)
     */
    @Transactional
    public AuthorizedUserDTO grantAuthorization(
            UUID childId, UUID grantorUserId, ChildAuthorizationDTO authorizationDTO
    ) {

        log.info("권한 부여 - childId: {}, grantorUserId: {}, targetUserId: {}",
                childId, grantorUserId, authorizationDTO.getUserId());

        // 1. 아동 조회
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수  없습니다"));

        // 2. 권한 부여자가 주 보호자인지 확인
        if (!child.isPrimaryParent(grantorUserId)) {
            throw new IllegalStateException("주보호자만 권한을 부여할 수 있습니다");
        }

        // 3. 대상 사용자 조회
        User targetUser = userRepository.findById(authorizationDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다"));

        // 4. 중복 권한 확인
        boolean alreadyExists = authorizedUserRepository.existsByChildAndUser(child, targetUser);
        if (alreadyExists) {
            throw new IllegalStateException("이미 권한이 부여된 사용자입니다");
        }

        // 5. 주보호자 권한 체크
        boolean isPrimary = Boolean.TRUE.equals(authorizationDTO.getIsPrimary());
        if (isPrimary) {
            // PARENT 역할만 주보호자 가능
            if (!targetUser.hasRole(UserRole.PARENT)) {
                throw new IllegalStateException("주보호자는 PARENT 역할만 가능합니다");
            }

            // 주보호자는 1명만 가능
            long primaryCount = authorizedUserRepository.countPrimaryByChildId(childId);
            if (primaryCount > 0) {
                throw new IllegalStateException("주보호자는 1명만 가능합니다");
            }
        }

        // 6. 권한 부여자 조회
        User grantor = userRepository.findById(grantorUserId)
                .orElseThrow(() -> new IllegalArgumentException("권한 부여자를 찾을 수 없습니다"));

        // 7. 권한 엔티티 생성
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
     * 권한 수정 (주보호자만 가능)
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

        // 1. 아동 조회
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다"));

        // 2. 권한 수정자가 주보호자인지 확인
        if (!child.isPrimaryParent(grantorUserId)) {
            throw new IllegalStateException("주보호자만 권한을 수정할 수 있습니다");
        }

        // 3. 기존 권한 조회
        ChildrenAuthorizedUser authorization = authorizedUserRepository
                .findByChildIdAndUserId(childId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("권한을 찾을 수 없습니다"));

        // 4. 주보호자는 권한 수정 불가
        if (Boolean.TRUE.equals(authorization.getIsPrimary())) {
            throw new IllegalStateException("주보호자의 권한은 수정할 수 없습니다");
        }

        // 5. 권한 업데이트
        authorization.clearPermissions();
        authorizationDTO.getPermissions().forEach(authorization::addPermission);

        log.info("권한 수정 완료 - childId: {}, targetUserId: {}", childId, targetUserId);

        return AuthorizedUserDTO.from(authorization);
    }

    /**
     * 권한 취소 (주보호자만 가능)
     */
    @Transactional
    public void revokeAuthorization(UUID childId, UUID grantorUserId, UUID targetUserId) {
        log.info("권한 취소 - childId: {}, grantorUserId: {}, targetUserId: {}",
                childId, grantorUserId, targetUserId);

        // 1. 아동 조회
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다"));

        // 2. 권한 취소자가 주보호자인지 확인
        if (!child.isPrimaryParent(grantorUserId)) {
            throw new IllegalStateException("주보호자만 권한을 취소할 수 있습니다");
        }

        // 3. 기존 권한 조회
        ChildrenAuthorizedUser authorization = authorizedUserRepository
                .findByChildIdAndUserId(childId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("권한을 찾을 수 없습니다"));

        // 4. 주보호자는 권한 취소 불가
        if (Boolean.TRUE.equals(authorization.getIsPrimary())) {
            throw new IllegalStateException("주보호자의 권한은 취소할 수 없습니다");
        }

        // 5. 권한 비활성화 (Soft Delete)
        authorization.deactivate();

        log.info("권한 취소 완료 - childId: {}, targetUserId: {}", childId, targetUserId);
    }

    /**
     * 특정 아동의 권한 목록 조회
     */
    public List<AuthorizedUserDTO> getAuthorizedUsers(UUID childId, UUID requestUserId) {
        log.info("권한 목록 조회 - childId: {}, requestUserId: {}", childId, requestUserId);

        // 1. 아동 조회
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다"));

        // 2. 조회자의 접근 권한 확인
        if (!child.canAccess(requestUserId)) {
            throw new IllegalStateException("해당 아동에 대한 접근 권한이 없습니다");
        }

        // 3. 활성화된 권한 목록 조회
        List<ChildrenAuthorizedUser> authorizations = authorizedUserRepository
                .findActiveByChildId(childId);

        return authorizations.stream()
                .map(AuthorizedUserDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 내가 주보호자인 아동 목록 조회
     */
    public List<UUID> getMyPrimaryChildrenIds(UUID userId) {
        log.info("내가 주보호자인 아동 ID 목록 조회 - userId: {}", userId);

        List<ChildrenAuthorizedUser> authorizations = authorizedUserRepository
                .findPrimaryByUserId(userId);

        return authorizations.stream()
                .map(au -> au.getChild().getChildId())
                .collect(Collectors.toList());
    }

    /**
     * 특정 권한을 가진 사용자 확인
     */
    public boolean hasPermission(UUID childId, UUID userId, ChildPermissionType permission) {
        return authorizedUserRepository.existsByChildIdAndUserIdAndPermission(
                childId, userId, permission
        );
    }
}
