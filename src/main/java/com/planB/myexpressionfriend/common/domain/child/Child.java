package com.planB.myexpressionfriend.common.domain.child;

import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 아동 엔티티
 *
 * 주의사항:
 * - parent 필드 제거: authorizedUsers에서 isPrimary=true로 주보호자 관리
 * - Soft Delete: @SQLRestriction으로 삭제된 데이터 자동 필터링
 */
@Entity
@Table(name = "children", indexes = {
        @Index(name = "idx_children_created", columnList = "created_at"),
        @Index(name = "idx_children_deleted", columnList = "is_deleted")
})
@SQLRestriction("is_deleted = false")  // Soft Delete 자동 필터링
@SQLDelete(sql = "UPDATE children SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE child_id = ?")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"authorizedUsers"})
@EntityListeners(AuditingEntityListener.class)
public class Child {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "child_id", updatable = false, nullable = false)
    private UUID childId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 10)
    private String gender;

    @Column(name = "diagnosis_date")
    private LocalDate diagnosisDate;

    /**
     * Parental Gate PIN (4자리, BCrypt 암호화)
     */
    @Column(name = "pin_code", length = 255)
    private String pinCode;

    /**
     * PIN 활성화 여부
     */
    @Column(name = "pin_enabled", nullable = false)
    @Builder.Default
    private Boolean pinEnabled = false;

    /**
     * 권한 부여된 사용자 목록
     * - 주보호자는 isPrimary=true로 구분
     * - parent 필드 제거하여 단일 진실 공급원(Single Source of Truth) 유지
     */
    @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ChildrenAuthorizedUser> authorizedUsers = new HashSet<>();

    /**
     * Soft Delete
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============= 조회 메서드 =============

    /**
     * 주보호자 조회
     */
    public Optional<User> getPrimaryParent() {
        return authorizedUsers.stream()
                .filter(au -> Boolean.TRUE.equals(au.getIsPrimary()))
                .filter(ChildrenAuthorizedUser::getIsActive)
                .map(ChildrenAuthorizedUser::getUser)
                .findFirst();
    }

    /**
     * 주보호자 ID 조회 (편의 메서드)
     */
    public Optional<UUID> getPrimaryParentId() {
        return getPrimaryParent()
                .map(User::getUserId);
    }

    // ============= 비즈니스 메서드 =============

    /**
     * 이름 변경
     */
    public void changeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다");
        }
        if (name.length() < 2 || name.length() > 50) {
            throw new IllegalArgumentException("이름은 2-50자 사이여야 합니다");
        }
        this.name = name.trim();
    }

    /**
     * 생년월일 변경
     */
    public void changeBirthDate(LocalDate birthDate) {
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("생년월일은 미래일 수 없습니다");
        }
        this.birthDate = birthDate;
    }

    /**
     * 성별 변경
     */
    public void changeGender(String gender) {
        if (gender != null && !gender.matches("^(MALE|FEMALE|OTHER)$")) {
            throw new IllegalArgumentException("성별은 MALE, FEMALE, OTHER 중 하나여야 합니다");
        }
        this.gender = gender;
    }

    /**
     * 진단일 변경
     */
    public void changeDiagnosisDate(LocalDate diagnosisDate) {
        if (diagnosisDate != null && diagnosisDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("진단일은 미래일 수 없습니다");
        }
        this.diagnosisDate = diagnosisDate;
    }

    // ============= PIN 관리 =============

    /**
     * PIN 설정 (BCrypt 암호화 필요)
     */
    public void setPinCode(String encryptedPin) {
        if (encryptedPin == null || encryptedPin.isEmpty()) {
            throw new IllegalArgumentException("암호화된 PIN은 필수입니다");
        }
        this.pinCode = encryptedPin;
        this.pinEnabled = true;
    }

    /**
     * PIN 검증
     */
    public boolean verifyPin(String rawPin, PasswordEncoder passwordEncoder) {
        if (!pinEnabled || pinCode == null) {
            return false;
        }
        return passwordEncoder.matches(rawPin, pinCode);
    }

    /**
     * PIN 제거
     */
    public void removePinCode() {
        this.pinCode = null;
        this.pinEnabled = false;
    }

    /**
     * PIN 활성화
     */
    public void enablePin() {
        if (this.pinCode == null) {
            throw new IllegalStateException("PIN이 설정되지 않았습니다");
        }
        this.pinEnabled = true;
    }

    /**
     * PIN 비활성화
     */
    public void disablePin() {
        this.pinEnabled = false;
    }

    // ============= 권한 관리 (Aggregate Root) =============

    /**
     * 권한 부여된 사용자 추가
     * 양방향 연관관계 편의 메서드
     */
    public void addAuthorizedUser(ChildrenAuthorizedUser authorizedUser) {
        if (authorizedUser == null) {
            throw new IllegalArgumentException("권한 정보는 필수입니다");
        }

        // 주보호자 중복 체크 (Aggregate Root에서 관리)
        if (Boolean.TRUE.equals(authorizedUser.getIsPrimary())) {
            validatePrimaryParentConstraint();

            // 주보호자는 PARENT 역할만 가능
            if (!authorizedUser.getUser().hasRole(UserRole.PARENT)) {
                throw new IllegalStateException("주보호자는 PARENT 역할만 가능합니다");
            }
        }

        // 양방향 연관관계 설정
        this.authorizedUsers.add(authorizedUser);
        if (authorizedUser.getChild() != this) {
            authorizedUser.setChild(this);
        }
    }

    /**
     * 주보호자 제약 검증
     * 이미 주보호자가 있으면 예외 발생
     */
    private void validatePrimaryParentConstraint() {
        long primaryCount = authorizedUsers.stream()
                .filter(au -> Boolean.TRUE.equals(au.getIsPrimary()))
                .filter(ChildrenAuthorizedUser::getIsActive)
                .count();

        if (primaryCount > 0) {
            throw new IllegalStateException("주보호자는 1명만 가능합니다");
        }
    }

    /**
     * 권한 부여된 사용자 제거
     */
    public void removeAuthorizedUser(ChildrenAuthorizedUser authorizedUser) {
        if (authorizedUser == null) {
            return;
        }

        // 주보호자는 제거 불가
        if (Boolean.TRUE.equals(authorizedUser.getIsPrimary())) {
            throw new IllegalStateException("주보호자는 제거할 수 없습니다. 먼저 다른 보호자를 주보호자로 지정하세요.");
        }

        this.authorizedUsers.remove(authorizedUser);
    }

    /**
     * 주보호자 변경 (양육권 이전)
     * 기존 주보호자 → 일반 권한으로 변경
     * 새 주보호자 → 주보호자 권한 부여
     */
    public void transferPrimaryParent(UUID newPrimaryParentUserId) {
        if (newPrimaryParentUserId == null) {
            throw new IllegalArgumentException("새 주보호자 ID는 필수입니다");
        }

        // 새 주보호자가 PARENT 역할인지 확인
        ChildrenAuthorizedUser newPrimary = authorizedUsers.stream()
                .filter(au -> au.getUser().getUserId().equals(newPrimaryParentUserId))
                .filter(ChildrenAuthorizedUser::getIsActive)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("새 주보호자가 권한 목록에 없습니다"));

        if (!newPrimary.getUser().hasRole(UserRole.PARENT)) {
            throw new IllegalStateException("주보호자는 PARENT 역할만 가능합니다");
        }

        // 기존 주보호자 해제
        authorizedUsers.stream()
                .filter(au -> Boolean.TRUE.equals(au.getIsPrimary()))
                .forEach(au -> au.setIsPrimary(false));

        // 새 주보호자 설정
        newPrimary.setIsPrimary(true);
        newPrimary.setAllPermissions();
    }

    /**
     * 특정 사용자가 특정 권한을 가지고 있는지 확인
     */
    public boolean hasPermission(UUID userId, ChildPermissionType permission) {
        return authorizedUsers.stream()
                .filter(au -> au.getUser().getUserId().equals(userId))
                .filter(ChildrenAuthorizedUser::getIsActive)
                .anyMatch(au -> au.hasPermission(permission));
    }

    /**
     * 주보호자 확인
     */
    public boolean isPrimaryParent(UUID userId) {
        return authorizedUsers.stream()
                .filter(au -> au.getUser().getUserId().equals(userId))
                .filter(ChildrenAuthorizedUser::getIsActive)
                .anyMatch(au -> Boolean.TRUE.equals(au.getIsPrimary()));
    }

    /**
     * 접근 권한 확인 (조회/수정 모두)
     */
    public boolean canAccess(UUID userId) {
        return authorizedUsers.stream()
                .filter(au -> au.getUser().getUserId().equals(userId))
                .filter(ChildrenAuthorizedUser::getIsActive)
                .anyMatch(au -> !au.getPermissions().isEmpty());
    }

    // ============= Soft Delete =============

    /**
     * Soft Delete
     * 관련 ChildrenAuthorizedUser도 함께 삭제됨 (cascade)
     */
    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 복구
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }
}