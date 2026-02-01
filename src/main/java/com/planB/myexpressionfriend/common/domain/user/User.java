package com.planB.myexpressionfriend.common.domain.user;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "password")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        //회원가입 시 기본 역할 강제 부여
        if (this.roles.isEmpty()) {
            this.roles.add(UserRole.PARENT);
        }
    }

    // ============= 비즈니스 메서드 =============

    /**
     * 이름 변경
     * @param name 새로운 이름
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
     * 비밀번호 변경
     * @param encryptedPassword 암호화된 비밀번호 (BCrypt)
     */
    public void changePassword(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다");
        }
        this.password = encryptedPassword;
    }

    /**
     * 이메일 변경
     * @param email 새로운 이메일
     */
    public void changeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다");
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다");
        }
        this.email = email.toLowerCase().trim();
    }

    /**
     * 프로필 수정 (이름 + 이메일 동시 변경)
     * @param name 새로운 이름 (null이면 변경 안 함)
     * @param email 새로운 이메일 (null이면 변경 안 함)
     */
    public void updateProfile(String name, String email) {
        if (name != null && !name.isBlank()) {
            changeName(name);  // 검증 포함
        }
        if (email != null && !email.isBlank()) {
            changeEmail(email);  // 검증 포함
        }
    }

    // ============= 역할 관리 =============

    /**
     * 역할 추가
     * @param role 추가할 역할
     */
    public void addRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("역할은 필수입니다");
        }
        this.roles.add(role);
    }

    /**
     * 역할 제거
     * @param role 제거할 역할
     */
    public void removeRole(UserRole role) {
        // PARENT 역할은 제거 불가 (최소 1개 역할 유지)
        if (role == UserRole.PARENT && this.roles.size() == 1) {
            throw new IllegalStateException("최소 1개의 역할은 유지되어야 합니다");
        }
        this.roles.remove(role);
    }

    /**
     * 모든 역할 초기화 (기본 역할로)
     */
    public void resetRoles() {
        this.roles.clear();
        this.roles.add(UserRole.PARENT);
    }

    /**
     * 특정 역할을 가지고 있는지 확인
     * @param role 확인할 역할
     * @return 소유 여부
     */
    public boolean hasRole(UserRole role) {
        return this.roles.contains(role);
    }

    /**
     * 관리자인지 확인
     * @return 관리자 여부
     */
    public boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    /**
     * 치료사인지 확인
     * @return 치료사 여부
     */
    public boolean isTherapist() {
        return hasRole(UserRole.THERAPIST);
    }
}
