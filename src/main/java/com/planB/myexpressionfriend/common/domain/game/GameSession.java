package com.planB.myexpressionfriend.common.domain.game;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 *
 */
@Entity
@Table(name = "game_sessions", indexes = {
        @Index(name = "idx_game_session_token", columnList = "session_token", unique = true),
        @Index(name = "idx_game_session_child", columnList = "child_id"),
        @Index(name = "idx_game_session_expires", columnList = "expires_at"),
        @Index(name = "idx_game_session_active", columnList = "is_active")
})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class GameSession {

    public static final int SESSION_DURATION_HOURS = 24;

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "session_id", updatable = false, nullable = false)
    private UUID sessionId;

    /**
     */
    @Column(name = "session_token", nullable = false, unique = true, length = 36)
    private String sessionToken;

    /**
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    /**
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authenticated_by", nullable = false)
    private User authenticatedBy;

    /**
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    // 게임 세션을 생성한 사용자(부모/치료사) UUID
    @CreatedBy
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    // terminate/extend 등 세션 상태를 마지막으로 변경한 사용자 UUID
    @LastModifiedBy
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "last_modified_by")
    private UUID lastModifiedBy;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    /**
     */
    public static GameSession create(Child child, User authenticatedBy) {
        return GameSession.builder()
                .sessionToken(UUID.randomUUID().toString())
                .child(child)
                .authenticatedBy(authenticatedBy)
                .expiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS))
                .isActive(true)
                .build();
    }

    /**
     * 세션 만료 여부를 확인합니다.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     */
    public boolean isValid() {
        return isActive && !isExpired();
    }

    /**
     */
    public void refresh() {
        if (!isValid()) {
            throw new IllegalStateException("만료되었거나 비활성화된 세션입니다.");
        }
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     */
    public void terminate() {
        this.isActive = false;
    }

    /**
     */
    public void extend() {
        if (!isActive) {
            throw new IllegalStateException("비활성화된 세션은 연장할 수 없습니다.");
        }
        this.expiresAt = LocalDateTime.now().plusHours(SESSION_DURATION_HOURS);
    }
}
