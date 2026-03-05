package com.planB.myexpressionfriend.common.domain.child;

import com.planB.myexpressionfriend.common.domain.user.User;
import com.planB.myexpressionfriend.common.domain.user.UserRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
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
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 아동 엔티티
 * - Soft Delete 적용(@SQLRestriction)
 */
@Entity
@Table(name = "children", indexes = {
        @Index(name = "idx_children_created", columnList = "created_at"),
        @Index(name = "idx_children_deleted", columnList = "is_deleted")
})
@SQLRestriction("is_deleted = false")
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

    @Column(name = "diagnosis_info", columnDefinition = "TEXT")
    private String diagnosisInfo;

    @Column(name = "special_notes", columnDefinition = "TEXT")
    private String specialNotes;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "child_preferred_expressions",
            joinColumns = @JoinColumn(name = "child_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "expression", length = 30)
    @Builder.Default
    private Set<ExpressionTag> preferredExpressions = new HashSet<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "child_difficult_expressions",
            joinColumns = @JoinColumn(name = "child_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "expression", length = 30)
    @Builder.Default
    private Set<ExpressionTag> difficultExpressions = new HashSet<>();

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /**
     */
    @Column(name = "pin_code", length = 255)
    private String pinCode;

    /**
     * PIN 사용 여부
     */
    @Column(name = "pin_enabled", nullable = false)
    @Builder.Default
    private Boolean pinEnabled = false;

    @Column(name = "pin_failed_attempts")
    @Builder.Default
    private Integer pinFailedAttempts = 0;

    @Column(name = "pin_locked_until")
    private LocalDateTime pinLockedUntil;

    /**
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


    /**
     */
    public Optional<User> getPrimaryParent() {
        return authorizedUsers.stream()
                .filter(au -> Boolean.TRUE.equals(au.getIsPrimary()))
                .filter(ChildrenAuthorizedUser::getIsActive)
                .map(ChildrenAuthorizedUser::getUser)
                .findFirst();
    }

    /**
     */
    public Optional<UUID> getPrimaryParentId() {
        return getPrimaryParent().map(User::getUserId);
    }


    /**
     */
    public void changeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        if (name.length() < 2 || name.length() > 50) {
            throw new IllegalArgumentException("이름은 2자 이상 50자 이하여야 합니다.");
        }
        this.name = name.trim();
    }

    /**
     */
    public void changeBirthDate(LocalDate birthDate) {
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("생년월일은 미래일 수 없습니다.");
        }
        this.birthDate = birthDate;
    }

    /**
     */
    public void changeGender(String gender) {
        if (gender != null && !gender.matches("^(MALE|FEMALE|OTHER)$")) {
            throw new IllegalArgumentException("성별은 MALE, FEMALE, OTHER 중 하나여야 합니다.");
        }
        this.gender = gender;
    }

    /**
     */
    public void changeDiagnosisDate(LocalDate diagnosisDate) {
        if (diagnosisDate != null && diagnosisDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("진단일은 미래일 수 없습니다.");
        }
        this.diagnosisDate = diagnosisDate;
    }

    /**
     */
    public void changeDiagnosisInfo(String diagnosisInfo) {
        if (diagnosisInfo != null && diagnosisInfo.length() > 3000) {
            throw new IllegalArgumentException("진단 정보는 3000자 이하여야 합니다.");
        }
        this.diagnosisInfo = diagnosisInfo;
    }

    /**
     */
    public void changeSpecialNotes(String specialNotes) {
        if (specialNotes != null && specialNotes.length() > 3000) {
            throw new IllegalArgumentException("특이사항은 3000자 이하여야 합니다.");
        }
        this.specialNotes = specialNotes;
    }

    /**
     */
    public void updatePreferredExpressions(Set<ExpressionTag> preferredExpressions) {
        this.preferredExpressions.clear();
        if (preferredExpressions != null) {
            this.preferredExpressions.addAll(preferredExpressions);
        }
    }

    /**
     */
    public void updateDifficultExpressions(Set<ExpressionTag> difficultExpressions) {
        this.difficultExpressions.clear();
        if (difficultExpressions != null) {
            this.difficultExpressions.addAll(difficultExpressions);
        }
    }

    /**
     */
    public void changeProfileImageUrl(String profileImageUrl) {
        if (profileImageUrl != null && profileImageUrl.length() > 500) {
            throw new IllegalArgumentException("프로필 이미지 URL은 500자 이하이어야 합니다.");
        }
        this.profileImageUrl = profileImageUrl;
    }

    /**
     */
    public Integer calculateAge() {
        if (this.birthDate == null) {
            return null;
        }
        if (this.birthDate.isAfter(LocalDate.now())) {
            return 0;
        }
        return Period.between(this.birthDate, LocalDate.now()).getYears();
    }


    /**
     */
    public void setPinCode(String encryptedPin) {
        if (encryptedPin == null || encryptedPin.isEmpty()) {
            throw new IllegalArgumentException("유효한 PIN 값이 필요합니다.");
        }
        this.pinCode = encryptedPin;
        this.pinEnabled = true;
    }

    /**
     */
    public boolean verifyPin(String rawPin, PasswordEncoder passwordEncoder) {
        if (!pinEnabled || pinCode == null) {
            return false;
        }

        if (pinLockedUntil != null && LocalDateTime.now().isBefore(pinLockedUntil)) {
            throw new IllegalStateException(
                    "PIN이 잠겨 있습니다. " + pinLockedUntil.format(DateTimeFormatter.ofPattern("HH:mm")) + " 이후 다시 시도해 주세요."
            );
        }

        boolean matches = passwordEncoder.matches(rawPin, pinCode);

        if (matches) {
            this.pinFailedAttempts = 0;
            this.pinLockedUntil = null;
        } else {
            this.pinFailedAttempts++;
            if (this.pinFailedAttempts >= 3) {
                this.pinLockedUntil = LocalDateTime.now().plusMinutes(5);
            }
        }

        return matches;
    }

    /**
     * PIN 제거
     */
    public void removePinCode() {
        this.pinCode = null;
        this.pinEnabled = false;
    }

    /**
     */
    public void enablePin() {
        if (this.pinCode == null) {
            throw new IllegalStateException("PIN이 설정되지 않아 활성화할 수 없습니다.");
        }
        this.pinEnabled = true;
    }

    /**
     */
    public void disablePin() {
        this.pinEnabled = false;
    }


    /**
     */
    public void addAuthorizedUser(ChildrenAuthorizedUser authorizedUser) {
        if (authorizedUser == null) {
            throw new IllegalArgumentException("권한 사용자 정보가 필요합니다.");
        }

        if (Boolean.TRUE.equals(authorizedUser.getIsPrimary())) {
            validatePrimaryParentConstraint();

            if (!authorizedUser.getUser().hasRole(UserRole.PARENT)) {
                throw new IllegalStateException("주보호자는 PARENT 권한 사용자만 가능합니다.");
            }
        }

        this.authorizedUsers.add(authorizedUser);
        if (authorizedUser.getChild() != this) {
            authorizedUser.setChild(this);
        }
    }

    /**
     */
    private void validatePrimaryParentConstraint() {
        long primaryCount = authorizedUsers.stream()
                .filter(au -> Boolean.TRUE.equals(au.getIsPrimary()))
                .filter(ChildrenAuthorizedUser::getIsActive)
                .count();

        if (primaryCount > 0) {
            throw new IllegalStateException("주보호자는 1명만 설정할 수 있습니다.");
        }
    }

    /**
     */
    public void removeAuthorizedUser(ChildrenAuthorizedUser authorizedUser) {
        if (authorizedUser == null) {
            return;
        }

        if (Boolean.TRUE.equals(authorizedUser.getIsPrimary())) {
            throw new IllegalStateException("주보호자는 직접 삭제할 수 없습니다. 먼저 주보호자 변경을 진행해 주세요.");
        }

        this.authorizedUsers.remove(authorizedUser);
    }

    /**
     */
    public void transferPrimaryParent(UUID newPrimaryParentUserId) {
        if (newPrimaryParentUserId == null) {
            throw new IllegalArgumentException("새 주보호자 ID가 필요합니다.");
        }

        ChildrenAuthorizedUser newPrimary = authorizedUsers.stream()
                .filter(au -> au.getUser().getUserId().equals(newPrimaryParentUserId))
                .filter(ChildrenAuthorizedUser::getIsActive)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("새 주보호자가 권한 사용자 목록에 없습니다."));

        if (!newPrimary.getUser().hasRole(UserRole.PARENT)) {
            throw new IllegalStateException("주보호자는 PARENT 권한 사용자만 가능합니다.");
        }

        authorizedUsers.stream()
                .filter(au -> Boolean.TRUE.equals(au.getIsPrimary()))
                .forEach(au -> au.setIsPrimary(false));

        newPrimary.setIsPrimary(true);
        newPrimary.setAllPermissions();
    }

    /**
     */
    public boolean hasPermission(UUID userId, ChildPermissionType permission) {
        return authorizedUsers.stream()
                .filter(au -> au.getUser().getUserId().equals(userId))
                .filter(ChildrenAuthorizedUser::getIsActive)
                .anyMatch(au -> au.hasPermission(permission));
    }

    /**
     */
    public boolean isPrimaryParent(UUID userId) {
        return authorizedUsers.stream()
                .filter(au -> au.getUser().getUserId().equals(userId))
                .filter(ChildrenAuthorizedUser::getIsActive)
                .anyMatch(au -> Boolean.TRUE.equals(au.getIsPrimary()));
    }

    /**
     */
    public boolean canAccess(UUID userId) {
        return authorizedUsers.stream()
                .filter(au -> au.getUser().getUserId().equals(userId))
                .filter(ChildrenAuthorizedUser::getIsActive)
                .anyMatch(au -> !au.getPermissions().isEmpty());
    }

    // ============= Soft Delete =============

    /**
     */
    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     */
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }
}
