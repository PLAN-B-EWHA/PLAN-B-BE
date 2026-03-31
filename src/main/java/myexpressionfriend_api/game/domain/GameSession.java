package myexpressionfriend_api.game.domain;

import jakarta.persistence.*;
import lombok.*;
import myexpressionfriend_api.auth.domain.user.User;
import myexpressionfriend_api.child.domain.Child;
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
 * 게임 세션 엔티티
 * - Unity 클라이언트와 아동을 연결하는 세션 토큰을 관리
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

    /** UUID 문자열 기반 세션 토큰 (Unity 클라이언트로 전달) */
    @Column(name = "session_token", nullable = false, unique = true, length = 36)
    private String sessionToken;

    /** 게임을 진행하는 아동 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    /** PIN 검증을 수행한 보호자/치료사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authenticated_by", nullable = false)
    private User authenticatedBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreatedBy
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

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

    // ============= 팩토리 메서드 =============

    public static GameSession create(Child child, User authenticatedBy) {
        return GameSession.builder()
                .sessionToken(UUID.randomUUID().toString())
                .child(child)
                .authenticatedBy(authenticatedBy)
                .expiresAt(LocalDateTime.now().plusHours(SESSION_DURATION_HOURS))
                .isActive(true)
                .build();
    }

    // ============= 도메인 로직 =============

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return isActive && !isExpired();
    }

    /** lastUsedAt 갱신 (세션 만료 연장은 하지 않음) */
    public void refresh() {
        if (!isValid()) {
            throw new IllegalStateException("만료되었거나 비활성화된 세션입니다.");
        }
        this.lastUsedAt = LocalDateTime.now();
    }

    public void terminate() {
        this.isActive = false;
    }

    public void extend() {
        if (!isActive) {
            throw new IllegalStateException("비활성화된 세션은 연장할 수 없습니다.");
        }
        this.expiresAt = LocalDateTime.now().plusHours(SESSION_DURATION_HOURS);
    }
}
