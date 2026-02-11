package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.dto.child.*;
import com.planB.myexpressionfriend.common.dto.game.GameSessionDTO;
import com.planB.myexpressionfriend.common.repository.ChildRepository;
import com.planB.myexpressionfriend.common.repository.ChildrenAuthorizedUserRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 아동 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ChildService {

    private final ChildRepository childRepository;
    private final ChildrenAuthorizedUserRepository authorizedUserRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GameSessionService gameSessionService;

    private static final int MAX_CHILDREN_PER_USER = 5; // 1인당 최대 아동 수

    /**
     * 아동 생성 (주보호자 자동 설정)
     */
    @Transactional
    public ChildDTO createChild(UUID parentUserId, ChildCreateDTO createDTO) {
        log.info("아동 생성 시작 - 부모 ID: {}", parentUserId);

        // 1. 부모 사용자 조회
        User parent = userRepository.findById(parentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다"));

        //  2. PARENT 역할 확인
        if (!parent.hasRole(UserRole.PARENT)) {
            throw new IllegalStateException("PARENT 역할만 아동을 생성할 수 있습니다");
        }

        // 3. 아동 수 제한 확인
        long childrenCount = childRepository.countByPrimaryParentUserId(parentUserId);
        if (childrenCount >= MAX_CHILDREN_PER_USER) {
            throw new IllegalStateException(
                    String.format("최대 %d명의 아동만 등록할 수 있습니다", MAX_CHILDREN_PER_USER)
            );
        }

        // 4. 아동 엔티티 생성
        Child child = Child.builder()
                .name(createDTO.getName())
                .birthDate(createDTO.getBirthDate())
                .gender(createDTO.getGender())
                .diagnosisDate(createDTO.getDiagnosisDate())
                .build();

        // 5. PIN 설정 (선택 사항)
        if (createDTO.getPin() !=null && !createDTO.getPin().isEmpty()) {

            String encryptedPin = passwordEncoder.encode(createDTO.getPin());
            child.setPinCode(encryptedPin);
            log.info("PIN 설정 완료");
        }

        // 6.  주보호자 권한 자동 부여
        ChildrenAuthorizedUser primaryAuthorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))  // 모든 권한
                .authorizedBy(parent)
                .isActive(true)
                .build();

        child.addAuthorizedUser(primaryAuthorization);
        childRepository.save(child);

        log.info("아동 생성 완료 - childId: {}, 주보호자: {}", child.getChildId(), parent.getEmail());

        return ChildDTO.from(child, parentUserId);
    }

    /**
     * 내 아동 목록 조회 (주보호자)
     */
    public List<ChildDTO> getMyChildren(UUID userId) {

        log.info("내 아동 목록 조회 - userID: {}", userId);

        List<Child> children = childRepository.findByPrimaryParentUserId(userId);

        return children.stream()
                .map(child -> ChildDTO.from(child, userId))
                .collect(Collectors.toList());
    }

    /**
     * 접근 가능한 아동 목록 (주보호자 + 권한 부여된 사용자)
     */
    public List<ChildDTO> getAccessibleChildren(UUID userId) {

        log.info("접근 가능한 아동 목록 조회 - userID: {}", userId);

        List<Child> children = childRepository.findAccessibleByUserId(userId);

        return children.stream()
                .map(child -> ChildDTO.from(child, userId))
                .collect(Collectors.toList());
    }

    /**
     * Unity 플레이 가능한 아동 목록 조회
     */
    public List<ChildDTO> getPlayableChildren(UUID userId) {
        log.info("플레이 가능한 아동 목록 조회 - userId: {}", userId);

        List<Child> children = childRepository.findAccessibleByUserId(userId);

        // PLAY_GAME 권한이 있는 아동만 필터링
        return children.stream()
                .filter(child -> child.hasPermission(userId, ChildPermissionType.PLAY_GAME))
                .map(child -> ChildDTO.from(child, userId))
                .collect(Collectors.toList());
    }

    /**
     * 아동 상세 조회
     */
    public ChildDetailDTO getChildDetail(UUID childId, UUID userId) {

        log.info("아동 상세 조회 - childId: {}, userID: {}", childId, userId);

        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다"));

        // 접근 권한 확인
        if (!child.canAccess(userId)) {
            throw new IllegalStateException("해당 아동에 대한 접근 권한이 없습니다");
        }

        return ChildDetailDTO.from(child);
    }

    /**
     * 아동 정보 수정 (주보호자 또는 MANAGE 권한 필요)
     */
    @Transactional
    public ChildDTO updateChild(UUID childId, UUID userId, ChildUpdateDTO updateDTO) {
        log.info("아동 정보 수정 - childId: {}, userId: {}", childId, userId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다"));

        // 권한 확인 (주보호자 또는 MANAGE 권한)
        if (!child.isPrimaryParent(userId) && !child.hasPermission(userId, ChildPermissionType.MANAGE)) {
            throw new IllegalStateException("아동 정보를 수정할 권한이 없습니다");
        }

        // 정보 업데이트
        if (updateDTO.getName() != null && !updateDTO.getName().isEmpty()) {
            child.changeName(updateDTO.getName());
        }

        if (updateDTO.getBirthDate() != null) {
            child.changeBirthDate(updateDTO.getBirthDate());
        }

        if (updateDTO.getGender() != null) {
            child.changeGender(updateDTO.getGender());
        }

        if (updateDTO.getDiagnosisDate() != null) {
            child.changeDiagnosisDate(updateDTO.getDiagnosisDate());
        }

        log.info("아동 정보 수정 완료 - childId: {}", childId);

        return ChildDTO.from(child, userId);
    }

    /**
     * 아동 삭제 (주보호자만 가능, Soft Delete)
     */
    @Transactional
    public void deleteChild(UUID childId, UUID userId) {
        log.info("아동 삭제 - childId: {}, userId: {}", childId, userId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다"));

        // 주보호자만 삭제 가능
        if (!child.isPrimaryParent(userId)) {
            throw new IllegalStateException("주보호자만 아동을 삭제할 수 있습니다");
        }

        // Soft Delete
        child.delete();

        log.info("아동 삭제 완료 (Soft Delete) - childId: {}", childId);
    }

    /**
     * PIN 설정/변경 (주보호자만 가능)
     */
    @Transactional
    public void updatePin(UUID childId, UUID userId, PinUpdateDTO pinUpdateDTO) {
        log.info("PIN 변경 - childId: {}, userId: {}", childId, userId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다"));

        // 주보호자만 변경 가능
        if (!child.isPrimaryParent(userId)) {
            throw new IllegalStateException("주보호자만 PIN을 설정할 수 있습니다");
        }

        // 현재 PIN 검증 (이미 설정된 경우)
        if (child.getPinEnabled()) {
            if (pinUpdateDTO.getCurrentPin() == null || pinUpdateDTO.getCurrentPin().isEmpty()) {
                throw new IllegalArgumentException("현재 PIN이 필요합니다");
            }

            if (!child.verifyPin(pinUpdateDTO.getCurrentPin(), passwordEncoder)) {
                throw new IllegalArgumentException("현재 PIN이 일치하지 않습니다");
            }
        }

        // 새 PIN 설정
        String encryptedPin = passwordEncoder.encode(pinUpdateDTO.getNewPin());
        child.setPinCode(encryptedPin);

        log.info("PIN 변경 완료 - childId: {}", childId);
    }

    /**
     * PIN 검증
     */
    public boolean verifyPin(UUID childId, UUID userId, PinVerificationDTO verificationDTO) {
        log.info("PIN 검증 - childId: {}, userId: {}", childId, userId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다"));

        // 접근 권한 확인
        if (!child.canAccess(userId)) {
            throw new IllegalStateException("해당 아동에 대한 접근 권한이 없습니다");
        }

        boolean isValid = child.verifyPin(verificationDTO.getPin(), passwordEncoder);

        log.info("PIN 검증 결과: {}", isValid);
        return isValid;
    }

    /**
     * PIN 검증 + 게임 세션 생성
     *
     * @param childId 아동 ID
     * @param userId 사용자 ID
     * @param verificationDTO PIN 검증 DTO
     * @return 게임 세션 DTO (세션 토큰 포함)
     */
    @Transactional
    public GameSessionDTO verifyPinAndCreateSession(
            UUID childId,
            UUID userId,
            PinVerificationDTO verificationDTO
    ) {
        log.info("PIN 검증 및 세션 생성 - childId: {}, userId: {}", childId, userId);

        // 1. PIN 검증
        boolean isValid = verifyPin(childId, userId, verificationDTO);

        if (!isValid) {
            throw new IllegalArgumentException("PIN이 일치하지 않습니다");
        }

        // 2. 게임 세션 생성
        GameSessionDTO session = gameSessionService.createSession(childId, userId);

        log.info("PIN 검증 및 세션 생성 완료 - sessionToken: {}", session.getSessionToken());

        return session;
    }

    /**
     * PIN 제거 (주보호자만 가능)
     */
    @Transactional
    public void removePin(UUID childId, UUID userId, String currentPin) {
        log.info("PIN 제거 - childId: {}, userId: {}", childId, userId);

        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다"));

        // 주보호자만 가능
        if (!child.isPrimaryParent(userId)) {
            throw new IllegalStateException("주보호자만 PIN을 제거할 수 있습니다");
        }

        // 현재 PIN 검증
        if (!child.verifyPin(currentPin, passwordEncoder)) {
            throw new IllegalArgumentException("현재 PIN이 일치하지 않습니다");
        }

        child.removePinCode();

        log.info("PIN 제거 완료 - childId: {}", childId);
    }

    /**
     * 주보호자 변경 (양육권 이전)
     */
    @Transactional
    public void transferPrimaryParent(UUID childId, UUID currentUserId, TransferPrimaryParentDTO transferDTO) {
        log.info("주보호자 변경 - childId: {}, currentUserId: {}, newPrimaryUserId: {}",
                childId, currentUserId, transferDTO.getNewPrimaryParentUserId());

        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다"));

        // 현재 주보호자만 변경 가능
        if (!child.isPrimaryParent(currentUserId)) {
            throw new IllegalStateException("주보호자만 양육권을 이전할 수 있습니다");
        }

        // PIN 검증 (설정된 경우)
        if (child.getPinEnabled()) {
            if (!child.verifyPin(transferDTO.getPin(), passwordEncoder)) {
                throw new IllegalArgumentException("PIN이 일치하지 않습니다");
            }
        }

        // 새 주보호자가 이미 권한이 있는지 확인
        boolean hasAuthorization = authorizedUserRepository.existsByChildIdAndUserIdAndPermission(
                childId,
                transferDTO.getNewPrimaryParentUserId(),
                ChildPermissionType.VIEW_REPORT
        );

        if (!hasAuthorization) {
            throw new IllegalArgumentException("새 주보호자가 해당 아동에 대한 권한이 없습니다. 먼저 권한을 부여하세요.");
        }

        // 주보호자 변경
        child.transferPrimaryParent(transferDTO.getNewPrimaryParentUserId());

        log.info("주보호자 변경 완료 - childId: {}, newPrimaryUserId: {}",
                childId, transferDTO.getNewPrimaryParentUserId());
    }
}
