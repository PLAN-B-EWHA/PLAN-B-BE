package com.planB.myexpressionfriend.common.domain.game;

import com.planB.myexpressionfriend.common.domain.child.Child;
import com.planB.myexpressionfriend.common.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 게임 세션 엔티티
 *
 * 역할:
 * - PIN 인증 후 발급되는 임시 게임 토큰
 * - 부모 JWT 대신 유니티에서 사용
 * - 제한된 권한 (게임 로그 전송만 가능)
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

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "session_id", updatable = false, nullable = false)
    private UUID sessionId;

    /**
     * 게임 세션 토큰 (UUID)
     */
    @Column(name = "session_token", nullable = false, unique = true, length = 36)
    private String sessionToken;

    /**
     * 아동 (게임 플레이 주체)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    /**
     * PIN 인증한 사용자 (부모 또는 치료사)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authenticated_by", nullable = false)
    private User authenticatedBy;

    /**
     * 세션 만료 시간 (기본: 24시간)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 세션 활성화 여부
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * 마지막 사용 시간
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ============= 비즈니스 메서드 =============

    /**
     * 게임 세션 생성 (24시간 유효)
     */
    public static GameSession create(Child child, User authenticatedBy) {
        return GameSession.builder()
                .sessionToken(UUID.randomUUID().toString())
                .child(child)
                .authenticatedBy(authenticatedBy)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .isActive(true)
                .build();
    }

    /**
     * 세션 만료 확인
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 세션 유효성 확인
     */
    public boolean isValid() {
        return isActive && !isExpired();
    }

    /**
     * 세션 갱신 (마지막 사용 시간 업데이트)
     */
    public void refresh() {
        if (!isValid()) {
            throw new IllegalStateException("만료되거나 비활성화된 세션입니다");
        }
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * 세션 종료
     */
    public void terminate() {
        this.isActive = false;
    }

    /**
     * 세션 만료 시간 연장 (추가 24시간)
     */
    public void extend() {
        if (!isActive) {
            throw new IllegalStateException("비활성화된 세션은 연장할 수 없습니다");
        }
        this.expiresAt = LocalDateTime.now().plusHours(24);
    }
}
