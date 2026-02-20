package com.planB.myexpressionfriend.common.domain.token;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Refresh Token 엔티티
 *
 * 사용자별로 하나의 유효한 Refresh Token만 유지
 * - 로그인 시: 생성 또는 업데이트
 * - 토큰 갱신 시: 검증 후 업데이트
 * - 로그아웃 시: 삭제
 */
@Entity
@Table(name = "refresh_tokens",
        indexes = {
            @Index(name = "idx_expires_at", columnList = "expires_at")
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    /**
     * Primary Key: User ID
     * 사용자당 하나의 Refresh Token만 유지
     */
    @Id
    @Column(name = "user_id", length = 36, nullable = false)
    private String userId;

    /**
     * JWT Refresh Token 문자열
     */
    @Column(name = "token", length = 500, nullable = false)
    private String token;

    /**
     * 토큰 만료 시간
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 생성 시간
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 시간 (토큰 갱신 시 업데이트)
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ============= 비즈니스 메서드 =============

    /**
     * 토큰 업데이트 (Token Rotation)
     *
     * @param newToken 새로운 Refresh Token
     * @param expirationMinutes 만료 시간 (분)
     */
    public void updateToken(String newToken, int expirationMinutes) {
        this.token = newToken;
        this.expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
    }

    /**
     * 토큰 만료 여부 확인
     *
     * @return 만료되었으면 true
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * 토큰 일치 여부 확인
     *
     * @param token 비교할 토큰
     * @return 일치하면 true
     */
    public boolean matchesToken(String token) {
        return this.token.equals(token);
    }
}
