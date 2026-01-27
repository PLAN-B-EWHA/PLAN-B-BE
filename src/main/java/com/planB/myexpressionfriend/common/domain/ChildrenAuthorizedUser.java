package com.planB.myexpressionfriend.common.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "children_authorized_users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"child_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_authorized_child", columnList = "child_id"),
                @Index(name = "idx_authorized_user", columnList = "user_id"),
                @Index(name = "idx_authorized_primary", columnList = "child_id, is_primary"),
                @Index(name = "idx_authorized_active", columnList = "is_active")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChildrenAuthorizedUser {

    @Id
    @GeneratedValue(generator = "uuid2")
    @Column(name = "authorization_id", updatable = false, nullable = false)
    private UUID authorizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    @Setter
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 권한 타입 (여러 개 가능)
     * 주의: Set.of()는 불변이므로 가변 컬렉션으로 변환 필요
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "authorized_user_permissions",
            joinColumns = @JoinColumn(name = "authorization_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "permission_type")
    private Set<ChildPermissionType> permissions = new HashSet<>();

    @Column(name = "is_primary", nullable = false)
    @Setter
    private Boolean isPrimary = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorized_by_user_id")
    private User authorizedBy;

    @CreatedDate
    @Column(name = "authorized_at", nullable = false, updatable = false)
    private LocalDateTime authorizedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // ============= 커스텀 빌더 (가변 컬렉션 변환) =============

    /**
     * 커스텀 빌더 클래스
     * Set.of()로 전달된 불변 컬렉션을 가변 HashSet으로 변환
     */
    public static class ChildrenAuthorizedUserBuilder {
        private Set<ChildPermissionType> permissions = new HashSet<>();

        /**
         * permissions 설정 시 가변 컬렉션으로 변환
         */
        public ChildrenAuthorizedUserBuilder permissions(Set<ChildPermissionType> permissions) {
            if (permissions != null) {
                this.permissions = new HashSet<>(permissions);  // 가변 컬렉션으로 복사
            }
            return this;
        }
    }

    // @Builder 어노테이션이 위의 커스텀 빌더를 사용하도록 함
    @Builder
    private ChildrenAuthorizedUser(
            UUID authorizationId,
            Child child,
            User user,
            Set<ChildPermissionType> permissions,
            Boolean isPrimary,
            User authorizedBy,
            LocalDateTime authorizedAt,
            Boolean isActive
    ) {
        this.authorizationId = authorizationId;
        this.child = child;
        this.user = user;
        // permissions는 빌더에서 이미 가변 컬렉션으로 변환됨
        this.permissions = (permissions != null) ? new HashSet<>(permissions) : new HashSet<>();
        this.isPrimary = (isPrimary != null) ? isPrimary : false;
        this.authorizedBy = authorizedBy;
        this.authorizedAt = authorizedAt;
        this.isActive = (isActive != null) ? isActive : true;
    }

    // ============= 비즈니스 메서드 =============

    public boolean hasPermission(ChildPermissionType permission) {
        if (Boolean.TRUE.equals(isPrimary)) {
            return true;
        }
        return permissions.contains(permission);
    }

    public void addPermission(ChildPermissionType permission) {
        if (permission == null) {
            throw new IllegalArgumentException("권한 타입은 필수입니다");
        }
        this.permissions.add(permission);
    }

    public void removePermission(ChildPermissionType permission) {
        if (Boolean.TRUE.equals(isPrimary)) {
            throw new IllegalStateException("주보호자의 권한은 제거할 수 없습니다");
        }
        this.permissions.remove(permission);
    }

    public void setAllPermissions() {
        this.permissions = new HashSet<>(Set.of(ChildPermissionType.values()));
    }

    public void clearPermissions() {
        if (Boolean.TRUE.equals(isPrimary)) {
            throw new IllegalStateException("주보호자의 권한은 제거할 수 없습니다");
        }
        this.permissions.clear();
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
}