package com.planB.myexpressionfriend.common.service;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.child.ChildPermissionType;
import com.planB.myexpressionfriend.common.domain.child.ChildrenAuthorizedUser;
import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import com.planB.myexpressionfriend.common.dto.child.ChildCreateDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildDetailDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildProfileUpdateDTO;
import com.planB.myexpressionfriend.common.dto.child.ChildUpdateDTO;
import com.planB.myexpressionfriend.common.dto.child.PinUpdateDTO;
import com.planB.myexpressionfriend.common.dto.child.PinVerificationDTO;
import com.planB.myexpressionfriend.common.dto.child.TransferPrimaryParentDTO;
import com.planB.myexpressionfriend.common.dto.game.GameSessionDTO;
import com.planB.myexpressionfriend.common.repository.ChildRepository;
import com.planB.myexpressionfriend.common.repository.ChildrenAuthorizedUserRepository;
import com.planB.myexpressionfriend.common.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 아동 서비스
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

    @Value("${app.storage.base-path:uploads}")
    private String storageBasePath;

    private static final int MAX_CHILDREN_PER_USER = 5;
    private static final long MAX_PROFILE_IMAGE_SIZE = 10L * 1024L * 1024L; // 10MB
    private static final Random PIN_RANDOM = new java.security.SecureRandom();

    @PostConstruct
    public void initStorage() {
        try {
            Files.createDirectories(Paths.get(storageBasePath));
        } catch (IOException e) {
            throw new RuntimeException("스토리지 디렉터리 초기화에 실패했습니다.", e);
        }
    }

    /**
     */
    @Transactional
    public ChildDTO createChild(UUID parentUserId, ChildCreateDTO createDTO) {
        User parent = userRepository.findById(parentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!parent.hasRole(UserRole.PARENT)) {
            throw new IllegalStateException("PARENT 권한만 아동을 생성할 수 있습니다.");
        }

        long childrenCount = childRepository.countByPrimaryParentUserId(parentUserId);
        if (childrenCount >= MAX_CHILDREN_PER_USER) {
            throw new IllegalStateException(String.format("최대 %d명까지 아동을 등록할 수 있습니다.", MAX_CHILDREN_PER_USER));
        }

        Child child = Child.builder()
                .name(createDTO.getName())
                .birthDate(createDTO.getBirthDate())
                .gender(createDTO.getGender())
                .diagnosisDate(createDTO.getDiagnosisDate())
                .build();

        if (createDTO.getDiagnosisInfo() != null) {
            child.changeDiagnosisInfo(createDTO.getDiagnosisInfo());
        }
        if (createDTO.getSpecialNotes() != null) {
            child.changeSpecialNotes(createDTO.getSpecialNotes());
        }
        if (createDTO.getPreferredExpressions() != null) {
            child.updatePreferredExpressions(createDTO.getPreferredExpressions());
        }
        if (createDTO.getDifficultExpressions() != null) {
            child.updateDifficultExpressions(createDTO.getDifficultExpressions());
        }
        if (createDTO.getProfileImageUrl() != null) {
            child.changeProfileImageUrl(createDTO.getProfileImageUrl());
        }

        if (createDTO.getPin() != null && !createDTO.getPin().isEmpty()) {
            child.setPinCode(passwordEncoder.encode(createDTO.getPin()));
        }

        ChildrenAuthorizedUser primaryAuthorization = ChildrenAuthorizedUser.builder()
                .child(child)
                .user(parent)
                .isPrimary(true)
                .permissions(Set.of(ChildPermissionType.values()))
                .authorizedBy(parent)
                .isActive(true)
                .build();

        child.addAuthorizedUser(primaryAuthorization);
        childRepository.save(child);
        return ChildDTO.from(child, parentUserId);
    }

    /**
     */
    public List<ChildDTO> getMyChildren(UUID userId) {
        return childRepository.findByPrimaryParentUserId(userId).stream()
                .map(child -> ChildDTO.from(child, userId))
                .collect(Collectors.toList());
    }

    /**
     */
    public List<ChildDTO> getAccessibleChildren(UUID userId) {
        return childRepository.findAccessibleByUserId(userId).stream()
                .map(child -> ChildDTO.from(child, userId))
                .collect(Collectors.toList());
    }

    /**
     */
    public List<ChildDTO> getPlayableChildren(UUID userId) {
        return childRepository.findAccessibleByUserId(userId).stream()
                .filter(child -> child.hasPermission(userId, ChildPermissionType.PLAY_GAME))
                .map(child -> ChildDTO.from(child, userId))
                .collect(Collectors.toList());
    }

    /**
     */
    public ChildDetailDTO getChildDetail(UUID childId, UUID userId) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.canAccess(userId)) {
            throw new IllegalStateException("해당 아동에 대한 접근 권한이 없습니다.");
        }

        return ChildDetailDTO.from(child);
    }

    /**
     * 아동 전체 정보 수정
     */
    @Transactional
    public ChildDTO updateChild(UUID childId, UUID userId, ChildUpdateDTO updateDTO) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        validateManagePermission(child, userId);

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
        if (updateDTO.getDiagnosisInfo() != null) {
            child.changeDiagnosisInfo(updateDTO.getDiagnosisInfo());
        }
        if (updateDTO.getSpecialNotes() != null) {
            child.changeSpecialNotes(updateDTO.getSpecialNotes());
        }
        if (updateDTO.getPreferredExpressions() != null) {
            child.updatePreferredExpressions(updateDTO.getPreferredExpressions());
        }
        if (updateDTO.getDifficultExpressions() != null) {
            child.updateDifficultExpressions(updateDTO.getDifficultExpressions());
        }
        if (updateDTO.getProfileImageUrl() != null) {
            child.changeProfileImageUrl(updateDTO.getProfileImageUrl());
        }

        return ChildDTO.from(child, userId);
    }

    /**
     */
    @Transactional
    public ChildDTO updateChildProfile(UUID childId, UUID userId, ChildProfileUpdateDTO updateDTO) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        validateManagePermission(child, userId);

        if (updateDTO.getDiagnosisInfo() != null) {
            child.changeDiagnosisInfo(updateDTO.getDiagnosisInfo());
        }
        if (updateDTO.getSpecialNotes() != null) {
            child.changeSpecialNotes(updateDTO.getSpecialNotes());
        }
        if (updateDTO.getPreferredExpressions() != null) {
            child.updatePreferredExpressions(updateDTO.getPreferredExpressions());
        }
        if (updateDTO.getDifficultExpressions() != null) {
            child.updateDifficultExpressions(updateDTO.getDifficultExpressions());
        }

        return ChildDTO.from(child, userId);
    }

    /**
     */
    @Transactional
    public ChildDTO uploadProfileImage(UUID childId, UUID userId, MultipartFile file) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        validateManagePermission(child, userId);
        validateProfileImageFile(file);

        String relativePath = buildProfileImageRelativePath(childId, file.getOriginalFilename());
        String oldProfileImageUrl = child.getProfileImageUrl();

        try {
            saveFile(file, relativePath);
            child.changeProfileImageUrl(relativePath);
            deleteFileQuietly(oldProfileImageUrl);
        } catch (IOException e) {
            throw new RuntimeException("프로필 이미지 업로드 중 오류가 발생했습니다.", e);
        }

        return ChildDTO.from(child, userId);
    }

    /**
     */
    @Transactional
    public ChildDTO deleteProfileImage(UUID childId, UUID userId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        validateManagePermission(child, userId);

        String oldProfileImageUrl = child.getProfileImageUrl();
        child.changeProfileImageUrl(null);
        deleteFileQuietly(oldProfileImageUrl);

        return ChildDTO.from(child, userId);
    }

    /**
     */
    @Transactional
    public void deleteChild(UUID childId, UUID userId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.isPrimaryParent(userId)) {
            throw new IllegalStateException("주 보호자만 아동을 삭제할 수 있습니다.");
        }

        child.delete();
    }

    /**
     */
    @Transactional
    public void updatePin(UUID childId, UUID userId, PinUpdateDTO pinUpdateDTO) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.isPrimaryParent(userId)) {
            throw new IllegalStateException("주 보호자만 PIN을 변경할 수 있습니다.");
        }

        if (child.getPinEnabled()) {
            if (pinUpdateDTO.getCurrentPin() == null || pinUpdateDTO.getCurrentPin().isEmpty()) {
                throw new IllegalArgumentException("현재 PIN이 필요합니다.");
            }

            if (!child.verifyPin(pinUpdateDTO.getCurrentPin(), passwordEncoder)) {
                throw new IllegalArgumentException("PIN이 일치하지 않습니다.");
            }
        }

        child.setPinCode(passwordEncoder.encode(pinUpdateDTO.getNewPin()));
    }

    /**
     */
    @Transactional
    public String issueTemporaryPin(UUID childId, UUID userId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.isPrimaryParent(userId)) {
            throw new IllegalStateException("주 보호자만 임시 PIN을 발급할 수 있습니다.");
        }

        String tempPin = String.format("%04d", PIN_RANDOM.nextInt(10000));
        child.setPinCode(passwordEncoder.encode(tempPin));
        return tempPin;
    }

    /**
     */
    public boolean verifyPin(UUID childId, UUID userId, PinVerificationDTO verificationDTO) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.canAccess(userId)) {
            throw new IllegalStateException("해당 아동에 대한 접근 권한이 없습니다.");
        }

        return child.verifyPin(verificationDTO.getPin(), passwordEncoder);
    }

    /**
     */
    @Transactional
    public GameSessionDTO verifyPinAndCreateSession(UUID childId, UUID userId, PinVerificationDTO verificationDTO) {
        boolean isValid = verifyPin(childId, userId, verificationDTO);
        if (!isValid) {
            throw new IllegalArgumentException("PIN이 일치하지 않습니다.");
        }
        return gameSessionService.createSession(childId, userId);
    }

    /**
     * PIN 제거
     */
    @Transactional
    public void removePin(UUID childId, UUID userId, String currentPin) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.isPrimaryParent(userId)) {
            throw new IllegalStateException("주 보호자만 PIN을 삭제할 수 있습니다.");
        }

        if (!child.verifyPin(currentPin, passwordEncoder)) {
            throw new IllegalArgumentException("PIN이 일치하지 않습니다.");
        }

        child.removePinCode();
    }

    /**
     */
    @Transactional
    public void transferPrimaryParent(UUID childId, UUID currentUserId, TransferPrimaryParentDTO transferDTO) {
        Child child = childRepository.findByIdWithAuthorizedUsers(childId)
                .orElseThrow(() -> new IllegalArgumentException("아동을 찾을 수 없습니다."));

        if (!child.isPrimaryParent(currentUserId)) {
            throw new IllegalStateException("주 보호자만 권한을 양도할 수 있습니다.");
        }

        if (child.getPinEnabled() && !child.verifyPin(transferDTO.getPin(), passwordEncoder)) {
            throw new IllegalArgumentException("PIN이 일치하지 않습니다.");
        }

        boolean hasAuthorization = authorizedUserRepository.existsByChildIdAndUserIdAndPermission(
                childId,
                transferDTO.getNewPrimaryParentUserId(),
                ChildPermissionType.VIEW_REPORT
        );

        if (!hasAuthorization) {
            throw new IllegalArgumentException("새 주 보호자에게 VIEW_REPORT 권한이 없습니다.");
        }

        child.transferPrimaryParent(transferDTO.getNewPrimaryParentUserId());
    }

    private void validateManagePermission(Child child, UUID userId) {
        if (!child.isPrimaryParent(userId) && !child.hasPermission(userId, ChildPermissionType.MANAGE)) {
            throw new IllegalStateException("아동 정보를 수정할 권한이 없습니다.");
        }
    }

    private void validateProfileImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지 파일이 필요합니다.");
        }
        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new IllegalArgumentException("프로필 이미지는 10MB 이하이어야 합니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
    }

    private String buildProfileImageRelativePath(UUID childId, String originalFileName) {
        return "children/" + childId + "/profile/" + UUID.randomUUID() + getFileExtension(originalFileName);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) {
            return ".jpg";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx == -1 || idx == fileName.length() - 1) {
            return ".jpg";
        }
        return fileName.substring(idx).toLowerCase();
    }

    private void saveFile(MultipartFile file, String relativePath) throws IOException {
        Path basePath = Paths.get(storageBasePath).normalize();
        Path filePath = Paths.get(storageBasePath, relativePath).normalize();

        if (!filePath.startsWith(basePath)) {
            throw new SecurityException("잘못된 파일 경로입니다.");
        }

        Files.createDirectories(filePath.getParent());
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void deleteFileQuietly(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }

        try {
            Path basePath = Paths.get(storageBasePath).normalize();
            Path filePath = Paths.get(storageBasePath, relativePath).normalize();
            if (!filePath.startsWith(basePath)) {
                return;
            }
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            log.warn("프로필 이미지 파일 삭제 실패: {}", relativePath);
        }
    }
}
