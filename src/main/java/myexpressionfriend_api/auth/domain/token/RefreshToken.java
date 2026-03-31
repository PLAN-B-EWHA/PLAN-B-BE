package myexpressionfriend_api.auth.domain.token;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens",
        indexes = {
            @Index(name = "idx_expire_at", columnList = "expires_at")
        })
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RefreshToken {

    @Id
    @JdbcTypeCode((SqlTypes.UUID))
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token", nullable = false,
            columnDefinition = "TEXT")
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    // ============= 비즈니스 메서드 =============

    /**
     * 토큰 업데이트 (Token Rotation)
     *
     * @param newToken      새로운 Refresh Token
     * @param newExpiresAt  새로운 만료 시간 (호출부에서 계산해 전달)
     */
    public void updateToken(String newToken, LocalDateTime newExpiresAt) {
        if (newToken == null || newToken.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }

        if (newExpiresAt == null || newExpiresAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("expiresAt must be in the future");
        }

        this.token = newToken;
        this.expiresAt = newExpiresAt;
    }

    /**
     * 토큰 만료 여부 확인
     *
     * @param now   기준 시간
     * @return      만료되었으면 true
     */
    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(this.expiresAt);
    }

    /**
     * 토큰 일치 여부 확인
     *
     * @param token 비교할 토큰
     * @return 일치하면 true
     */
    public boolean matchesToken(String token) {
        return this.token != null && this.token.equals(token);  // ✅ 3번: null 방어 추가
    }
}
